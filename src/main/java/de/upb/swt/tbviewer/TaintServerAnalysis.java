/*
 * @author Linghui Luo
 */
package de.upb.swt.tbviewer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.util.collections.Pair;
import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.Attribute;
import de.foellix.aql.datastructure.Flow;
import de.foellix.aql.datastructure.Flows;
import de.foellix.aql.datastructure.Parameter;
import de.foellix.aql.datastructure.Parameters;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.datastructure.handler.AnswerHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamException;
import magpiebridge.core.AnalysisConsumer;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.Kind;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.projectservice.java.JavaProjectService;
import magpiebridge.util.SourceCodeInfo;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;

/** @author Linghui Luo */
public class TaintServerAnalysis implements ServerAnalysis {

  private String rootPath; // The project root path
  private String groundTruthPath; // Path to JSON-file in TAF-format defines the ground truth.
  private String
      aqlResultPath; // Path to XML-file in AQLResult-format defines the taint analysis analysis
  // results.
  private String fdResultPath;

  private Map<String, TreeMap<Integer, Pair<Reference, Reference>>>
      loadedAqlResults; // <id ,TreeMap<Step, Flow>>
  private Map<TaintFlow, AnalysisResult> groundTruthFlows;
  private Map<Location, Pair<SourceCodePosition, String>> groundTruthLocations;
  private Collection<AnalysisResult> groundTruth;

  // the following three list needs to be cleared every time readAqlAnswer is
  // called.
  private ArrayList<AnalysisResult> aqlResults;
  private HashSet<AnalysisResult> matchedResults;
  private HashSet<AnalysisResult> nonMatchedResults;

  private TaintLanguageServer server;

  public TaintServerAnalysis() {
    this.groundTruth = new ArrayList<AnalysisResult>();
    this.aqlResults = new ArrayList<AnalysisResult>();
    this.matchedResults = new HashSet<AnalysisResult>();
    this.nonMatchedResults = new HashSet<AnalysisResult>();
    this.groundTruthLocations = new HashMap<>();
    this.loadedAqlResults = new HashMap<>();
    this.groundTruthFlows = new HashMap<>();
  }

  /** read aql results for the current project. */
  protected void loadAqlResults() {
    File xmlFile = new File(this.aqlResultPath);
    Answer answer = AnswerHandler.parseXML(xmlFile);
    Flows flows = answer.getFlows();
    int i = 0;
    for (Flow flow : flows.getFlow()) {
      List<Reference> refs = flow.getReference();
      String appFile = null;
      String apkName = null;
      Reference from = null;
      Reference to = null;
      for (Reference ref : refs) {
        String type = ref.getType();
        if (type.equals("from")) {
          appFile = ref.getApp().getFile();
          String[] splitsWindows = appFile.split(Pattern.quote(File.separator));
          String[] splitsLinux = appFile.split(Pattern.quote("/"));
          String[] splits = splitsWindows.length > splitsLinux.length ? splitsWindows : splitsLinux;
          if (splits.length > 0) {
            apkName = splits[splits.length - 1].split(".apk")[0];
          }

          from = ref;
        } else if (type.equals("to")) {
          to = ref;
        }
      }
      if (from != null && to != null) {
        Pair<Reference, Reference> res = Pair.make(from, to);
        i++;
        String id = i + "";
        String step = "1";
        if (flow.getAttributes() != null)
          for (Attribute a : flow.getAttributes().getAttribute()) {
            if (a.getName().equals("FlowID")) id = a.getValue();
            if (a.getName().equals("FlowStep")) step = a.getValue();
          }
        Integer istep = Integer.parseInt(step);
        if (!this.loadedAqlResults.containsKey(id)) this.loadedAqlResults.put(id, new TreeMap<>());
        this.loadedAqlResults.get(id).put(istep, res);
      }
    }
  }

  /**
   * Read ground truth from json file and encodes each finding as {@link Diagnostic} with {@link
   * DiagnosticSeverity#Error}.
   */
  protected void readGroundTruth() {
    try {
      File file = new File(this.groundTruthPath);
      JsonParser parser = new JsonParser();
      InputStreamReader reader =
          new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
      JsonObject obj = parser.parse(reader).getAsJsonObject();
      JsonArray findings = obj.getAsJsonArray("findings");
      for (int i = 0; i < findings.size(); i++) {
        ArrayList<Pair<Position, String>> related = new ArrayList<Pair<Position, String>>();
        ArrayList<Pair<Position, String>> relatedWithoutPath =
            new ArrayList<Pair<Position, String>>();
        JsonObject finding = findings.get(i).getAsJsonObject();
        boolean isNegativeFlow = false;
        if (finding.has("isNegative")) isNegativeFlow = finding.get("isNegative").getAsBoolean();
        else if (finding.has("isUnexpected")) {
          isNegativeFlow = finding.get("isUnexpected").getAsBoolean();
        }
        String ID = finding.get("ID").toString();
        StringBuilder message = new StringBuilder(ID + "P. Malicious taint flow");
        if (isNegativeFlow) message = new StringBuilder(ID + "N. Negative taint flow");
        String description = finding.get("description").getAsString();
        // process source info
        JsonObject source = finding.getAsJsonObject("source");
        String sourceClassName = source.get("className").getAsString();
        String sourceMethodName = source.get("methodName").getAsString();
        URL sourceUrl = classNameToURL(sourceClassName);
        String sourceStatement = source.get("statement").getAsString();
        String sourceTargetName = source.get("targetName").getAsString();
        int sourceLn = source.get("lineNo").getAsInt();
        Location sourceLoc =
            new Location(
                LocationKind.Java, sourceClassName, sourceMethodName, sourceStatement, sourceLn);
        SourceCodePosition sourcePos = null;
        if (source.has("decompiledSourceLineNo")) {
          int decompiledSourceLineNo = source.get("decompiledSourceLineNo").getAsInt();
          sourcePos =
              SourceCodePositionFinder.getFromLineNo(
                  sourceUrl, sourceStatement, decompiledSourceLineNo);
          try {
            SourceCodeInfo info =
                magpiebridge.util.SourceCodePositionFinder.findCode(
                    Paths.get(sourceUrl.toURI()).toFile(), decompiledSourceLineNo);
            if (info != null) {
              sourceStatement = info.code;
            }
          } catch (URISyntaxException e) {
            e.printStackTrace();
          }
        } else {
          sourcePos = SourceCodePositionFinder.get(sourceUrl, sourceMethodName, sourceStatement);
        }
        this.groundTruthLocations.put(sourceLoc, Pair.make(sourcePos, sourceStatement));
        if (sourcePos != null) {
          Pair<Position, String> sourceInfo = Pair.make(sourcePos, "SOURCE: " + sourceStatement);
          related.add(sourceInfo);
          relatedWithoutPath.add(sourceInfo);
        }
        // process sink info
        JsonObject sink = finding.getAsJsonObject("sink");

        String sinkStatement = sink.get("statement").getAsString();
        String sinkTargetName = sink.get("targetName").getAsString();
        String sinkClassName = sink.get("className").getAsString();
        String sinkMethodName = sink.get("methodName").getAsString();
        URL sinkUrl = classNameToURL(sinkClassName);
        SourceCodePosition sinkPos =
            SourceCodePositionFinder.get(sinkUrl, sinkMethodName, sinkStatement);
        if (sink.has("decompiledSourceLineNo")) {
          int decompiledSourceLineNo = sink.get("decompiledSourceLineNo").getAsInt();
          sinkPos =
              SourceCodePositionFinder.getFromLineNo(
                  sinkUrl, sinkStatement, decompiledSourceLineNo);
          try {
            SourceCodeInfo info =
                magpiebridge.util.SourceCodePositionFinder.findCode(
                    Paths.get(sinkUrl.toURI()).toFile(), decompiledSourceLineNo);
            if (info != null) {
              sinkStatement = info.code;
            }
          } catch (URISyntaxException e) {
            e.printStackTrace();
          }

        } else {
          sinkPos = SourceCodePositionFinder.get(sinkUrl, sinkMethodName, sinkStatement);
        }

        int sinkLn = sink.get("lineNo").getAsInt();
        Location sinkLoc =
            new Location(LocationKind.Java, sinkClassName, sinkMethodName, sinkStatement, sinkLn);
        this.groundTruthLocations.put(sinkLoc, Pair.make(sinkPos, sinkStatement));

        TaintFlow taintFlow = new TaintFlow(sourceLoc, sinkLoc);

        message.append(" to ");
        message.append(sinkStatement);
        message.append(" from ");
        message.append(sourceStatement);

        // process intermediate info
        JsonArray intermediateFlows = finding.getAsJsonArray("intermediateFlows");
        ArrayList<Position> intermediatePos = new ArrayList<>();
        for (int j = 0; j < intermediateFlows.size(); j++) {
          JsonObject flow = intermediateFlows.get(j).getAsJsonObject();
          if (flow.get("className") != null) {
            String flowClassName = flow.get("className").getAsString();
            URL flowUrl = classNameToURL(flowClassName);
            String flowMethodName = flow.get("methodName").getAsString();
            String flowStatement = flow.get("statement").getAsString();
            SourceCodePosition flowPos = null;
            if (flow.has("decompiledSourceLineNo")) {
              int decompiledSourceLineNo = flow.get("decompiledSourceLineNo").getAsInt();
              flowPos =
                  SourceCodePositionFinder.getFromLineNo(
                      flowUrl, flowStatement, decompiledSourceLineNo);
              try {
                SourceCodeInfo info =
                    magpiebridge.util.SourceCodePositionFinder.findCode(
                        Paths.get(flowUrl.toURI()).toFile(), decompiledSourceLineNo);
                if (info != null) flowStatement = info.code;
              } catch (URISyntaxException e) {
                e.printStackTrace();
              }
            } else {
              flowPos = SourceCodePositionFinder.get(flowUrl, flowMethodName, flowStatement);
            }
            int flowLn = flow.get("lineNo").getAsInt();
            Location flowLoc =
                new Location(
                    LocationKind.Java, flowClassName, flowMethodName, flowStatement, flowLn);
            this.groundTruthLocations.put(flowLoc, Pair.make(flowPos, flowStatement));
            if (flowPos != null) {
              intermediatePos.add(flowPos);
              Pair<Position, String> flowInfo = Pair.make(flowPos, flowStatement);
              related.add(flowInfo);
            }
          }
        }

        StringBuilder atrs = new StringBuilder();
        JsonObject attributes = finding.getAsJsonObject("attributes");
        if (attributes.entrySet().size() > 0) atrs.append("| ");
        for (Entry<String, JsonElement> attribute : attributes.entrySet()) {
          if (attribute.getValue().getAsBoolean()) {
            atrs.append(attribute.getKey());
            atrs.append(" | ");
          }
        }
        // reporting
        if (sinkPos != null) {
          Pair<Position, String> sinkInfo = Pair.make(sinkPos, "SINK: " + sinkStatement);
          related.add(sinkInfo);
          message.append(".");
          message.append("\n[Source] ");
          message.append(sourceTargetName);
          message.append("\n[Sink] ");
          message.append(sinkTargetName);
          message.append("\n[Description] ");
          message.append(description);
          if (!isNegativeFlow) {
            message.append("\n[Attributes] ");
            message.append(atrs.toString());
          }
          String msg = message.toString().replace(";", "");
          TaintAnalysisResult sinkResult = null;
          if (!isNegativeFlow) {
            sinkResult =
                new TaintAnalysisResult(
                    Optional.of(finding),
                    Kind.Diagnostic,
                    sinkPos,
                    msg,
                    related,
                    DiagnosticSeverity.Error,
                    null,
                    null);
          } else {
            sinkResult =
                new TaintAnalysisResult(
                    Optional.of(finding),
                    Kind.Diagnostic,
                    sinkPos,
                    msg,
                    related,
                    DiagnosticSeverity.Information,
                    null,
                    null);
          }
          this.groundTruthFlows.put(taintFlow, sinkResult);
          this.groundTruth.add(sinkResult);
        }
      }

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  protected List<String> getConstantParameters(Parameters ps) {
    List<String> constants = new ArrayList<>();
    if (ps != null)
      for (Parameter p : ps.getParameter()) {
        String value = p.getValue();
        if (value != null) {
          if (p.getType().equals("java.lang.String")) {
            if (value.startsWith("\"") && value.endsWith("\"")) {
              constants.add(value.substring(1, value.length() - 1));
            }
          } else {
            Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
            if (value.equals("null") || pattern.matcher(value).matches()) constants.add(value);
          }
        }
      }
    return constants;
  }

  protected Set<Pair<SourceCodePosition, String>> getPossibleSourceCodePosition(Reference node) {
    String stmt = node.getStatement().getStatementgeneric();
    int Ln = -1;
    if (node.getStatement().getLinenumber() != null) Ln = node.getStatement().getLinenumber();
    String hostMethod = node.getMethod().replace("<", "").replace(">", "");
    String hostClass = node.getClassname();
    Location location = new Location(LocationKind.Jimple, hostClass, hostMethod, stmt, Ln);
    List<String> constantParams = getConstantParameters(node.getStatement().getParameters());
    URL sourceUrl = classNameToURL(hostClass);
    Optional<Pair<SourceCodePosition, String>> op = findLocationMatch(location);
    Set<Pair<SourceCodePosition, String>> possibleCodePositions = new HashSet<>();
    if (op.isPresent()) possibleCodePositions.add(op.get());
    else {
      if (sourceUrl != null) {
        possibleCodePositions.addAll(
            SourceCodePositionFinder.getWithRegex(sourceUrl, hostMethod, stmt, constantParams));
      }
    }
    return possibleCodePositions;
  }

  /**
   * Read aql answer from xml file and encodes each finding as {@link Diagnostic} with {@link
   * DiagnosticSeverity#Information}.
   */
  protected void readAqlResults() {
    this.aqlResults.clear();
    this.matchedResults.clear();
    this.nonMatchedResults.clear();
    for (Entry<String, TreeMap<Integer, Pair<Reference, Reference>>> entry :
        this.loadedAqlResults.entrySet()) {
      String id = entry.getKey() + "D";
      TreeMap<Integer, Pair<Reference, Reference>> flow = entry.getValue();
      ArrayList<Pair<Position, String>> related = new ArrayList<Pair<Position, String>>();
      // process source info
      Reference source = flow.firstEntry().getValue().fst;
      Set<Pair<SourceCodePosition, String>> possibleSourceInfos =
          getPossibleSourceCodePosition(source);
      String prefix = "SOURCE: ";
      if (possibleSourceInfos.size() > 1) {
        prefix = "SOURCE (AMBIGUOUS): ";
      }
      for (Pair<SourceCodePosition, String> sourceInfo : possibleSourceInfos) {
        related.add(Pair.make(sourceInfo.fst, prefix + sourceInfo.snd));
      }

      // process intermediate steps
      if (flow.size() > 1) {
        for (Entry<Integer, Pair<Reference, Reference>> step : flow.entrySet()) {
          if (!step.equals(flow.lastEntry())) {
            Set<Pair<SourceCodePosition, String>> possibleIntermediateInfos =
                getPossibleSourceCodePosition(step.getValue().snd);
            prefix = "";
            if (possibleIntermediateInfos.size() > 1) {
              prefix = "(AMBIGUOUS) ";
            }
            for (Pair<SourceCodePosition, String> interInfo : possibleIntermediateInfos) {
              related.add(Pair.make(interInfo.fst, prefix + interInfo.snd));
            }
          }
        }
      }

      // process sink info
      Reference sink = flow.lastEntry().getValue().snd;
      Set<Pair<SourceCodePosition, String>> possibleSinkInfos = getPossibleSourceCodePosition(sink);

      String sinkMsg =
          id
              + ". Detected taint flow to "
              + sink.getStatement().getStatementgeneric()
              + " from "
              + source.getStatement().getStatementgeneric();
      prefix = "";
      if (possibleSinkInfos.size() > 1) {
        prefix = "(AMBIGUOUS) ";
      }
      sinkMsg = prefix + sinkMsg;
      for (Pair<SourceCodePosition, String> sinkInfo : possibleSinkInfos) {
        if (sinkInfo != null) {
          related.add(Pair.make(sinkInfo.fst, "SINK: " + sinkInfo.snd));
          String matchedFlowID = null;
          // for (Pair<SourceCodePosition, String> possibleSource : possibleSourceInfos) {
          // search if there is match in ground truth
          // Optional<AnalysisResult> res = findFlowMatchUsingSourceCode(possibleSource,
          // sinkInfo);

          // if (res.isPresent()) {
          // matchedFlowID = res.get().toString(false).split("\\.")[0];

          // }
          // }
          // search if there is match in ground truth
          Map<AnalysisResult, Boolean> res = findFlowMatchUsingJimple(source, sink);
          if (!res.isEmpty()) {
            for (Entry<AnalysisResult, Boolean> pair : res.entrySet()) {
              String certain = "";
              if (!pair.getValue()) {
                certain = "?";
              }
              if (matchedFlowID == null) {
                matchedFlowID = certain + pair.getKey().toString(false).split("\\.")[0];
              } else {
                matchedFlowID += ", " + certain + pair.getKey().toString(false).split("\\.")[0];
              }
            }
          }
          if (matchedFlowID != null) {
            String detectedFlowID = sinkMsg.split("\\.")[0];
            sinkMsg = sinkMsg.replace(detectedFlowID, detectedFlowID + " [" + matchedFlowID + "]");
          }
          TaintAnalysisResult aqlresult =
              new TaintAnalysisResult(
                  Kind.Diagnostic,
                  sinkInfo.fst,
                  sinkMsg,
                  related,
                  DiagnosticSeverity.Warning,
                  null,
                  null);
          aqlResults.add(aqlresult);
          if (matchedFlowID != null) {
            if (!(matchedFlowID.contains("N") && matchedFlowID.contains("P")))
              this.matchedResults.add(aqlresult);
          }
        }
      }
    }
  }

  /** Find ground truth location which matches the aqlLocation. */
  protected Optional<Pair<SourceCodePosition, String>> findLocationMatch(Location aqlLocation) {
    for (Location loc : this.groundTruthLocations.keySet()) {
      if (Location.maybeEqual(loc, aqlLocation, true, 1)) {
        Pair<SourceCodePosition, String> pos = this.groundTruthLocations.get(loc);
        if (pos.fst != null) return Optional.of(pos);
        else Optional.empty();
      }
    }
    return Optional.empty();
  }

  /**
   * Find ground truth flow which matches the aqlflow.
   *
   * @param aqlflow
   * @return
   */
  protected Optional<AnalysisResult> findFlowMatchUsingSourceCode(
      Pair<SourceCodePosition, String> source, Pair<SourceCodePosition, String> sink) {
    for (AnalysisResult groundTruthFlow : this.groundTruthFlows.values()) {
      Position sinkP = groundTruthFlow.position();
      Pair<Position, String> sourceP = groundTruthFlow.related().iterator().next();
      if (sinkP.equals(sink.fst) && sourceP.fst.equals(source.fst)) {
        AnalysisResult res = groundTruthFlow;
        return Optional.of(res);
      }
    }
    return Optional.empty();
  }

  protected Map<AnalysisResult, Boolean> findFlowMatchUsingJimple(
      Reference source, Reference sink) {
    Map<AnalysisResult, Boolean> res = new HashMap<>();
    for (AnalysisResult groundTruthFlow : this.groundTruthFlows.values()) {
      TaintAnalysisResult flow = (TaintAnalysisResult) groundTruthFlow;
      if (flow.getFinding().isPresent()) {

        JsonObject finding = flow.getFinding().get();
        JsonObject jsonsource = finding.getAsJsonObject("source");
        String sourceClassName = jsonsource.get("className").getAsString();
        String sourceMethodName = jsonsource.get("methodName").getAsString();
        int sourceLineNo = jsonsource.get("lineNo").getAsInt();
        String sourceJimpleStatement = null;
        if (jsonsource.has("IRs") && jsonsource.get("IRs").getAsJsonArray().size() > 0) {
          JsonObject IR = jsonsource.get("IRs").getAsJsonArray().get(0).getAsJsonObject();
          if (IR.get("type").getAsString().equals("Jimple")) {
            sourceJimpleStatement = IR.get("IRstatement").getAsString();
          }
        }
        JsonObject jsonsink = finding.getAsJsonObject("sink");
        String sinkClassName = jsonsink.get("className").getAsString();
        String sinkMethodName = jsonsink.get("methodName").getAsString();
        int sinkLineNo = jsonsink.get("lineNo").getAsInt();
        String sinkJimpleStatement = null;
        if (jsonsource.has("IRs") && jsonsink.get("IRs").getAsJsonArray().size() > 0) {
          JsonObject IR = jsonsink.get("IRs").getAsJsonArray().get(0).getAsJsonObject();
          if (IR.get("type").getAsString().equals("Jimple")) {
            sinkJimpleStatement = IR.get("IRstatement").getAsString();
          }
        }
        if (sourceJimpleStatement != null && sinkJimpleStatement != null) {
          if (source.getStatement().getLinenumber() == -1
              && sink.getStatement().getLinenumber() == -1) {
            // the xml result doesn't contain line numbers.
            sourceLineNo = -1;
            sinkLineNo = -1;
          }
          Location jsonSource =
              new Location(
                  LocationKind.Java, sourceClassName, sourceMethodName, null, sourceLineNo);
          Location jsonSink =
              new Location(LocationKind.Java, sinkClassName, sinkMethodName, null, sinkLineNo);
          Location xmlSource =
              new Location(
                  LocationKind.Jimple,
                  source.getClassname(),
                  source.getMethod(),
                  null,
                  source.getStatement().getLinenumber());
          Location xmlSink =
              new Location(
                  LocationKind.Jimple,
                  sink.getClassname(),
                  sink.getMethod(),
                  null,
                  sink.getStatement().getLinenumber());
          String xmlSourceStmt = source.getStatement().getStatementfull();
          String xmlSinkStmt = sink.getStatement().getStatementfull();
          if (Location.maybeEqual(jsonSource, xmlSource, false, 3)
              && Location.maybeEqual(jsonSink, xmlSink, false, 3)) {
            if (source.getStatement().getLinenumber() != -1
                && sink.getStatement().getLinenumber() != -1) {
              // also compared line numbers in maybeEqual,
              if (compareGenericStmts(
                  sourceJimpleStatement, sinkJimpleStatement, xmlSourceStmt, xmlSinkStmt)) {
                res.put(groundTruthFlow, true);
              }
            } else {
              // no line numbers are available
              if (sourceJimpleStatement.equals(xmlSourceStmt)
                  && sinkJimpleStatement.equals(xmlSinkStmt)) {
                res.put(groundTruthFlow, true);
              } else {
                if (compareGenericStmts(
                    sourceJimpleStatement, sinkJimpleStatement, xmlSourceStmt, xmlSinkStmt)) {
                  res.put(groundTruthFlow, false);
                }
              }
            }
          }
        }
      }
    }
    return res;
  }

  private boolean compareGenericStmts(
      String sourceJimpleStatement,
      String sinkJimpleStatement,
      String xmlSourceStmt,
      String xmlSinkStmt) {
    sourceJimpleStatement = sourceJimpleStatement.replaceAll("\\$r[0-9]+", "\\$rX");
    sourceJimpleStatement = sourceJimpleStatement.replaceAll("r[0-9]+", "\\$rX");
    sinkJimpleStatement = sinkJimpleStatement.replaceAll("\\$r[0-9]+", "\\$rX");
    sinkJimpleStatement = sinkJimpleStatement.replaceAll("r[0-9]+", "\\$rX");
    xmlSourceStmt = xmlSourceStmt.replaceAll("\\$r[0-9]+", "\\$rX");
    xmlSourceStmt = xmlSourceStmt.replaceAll("r[0-9]+", "\\$rX");
    xmlSinkStmt = xmlSinkStmt.replaceAll("\\$r[0-9]+", "\\$rX");
    xmlSinkStmt = xmlSinkStmt.replaceAll("r[0-9]+", "\\$rX");
    return sourceJimpleStatement.equals(xmlSourceStmt) && sinkJimpleStatement.equals(xmlSinkStmt);
  }

  protected URL classNameToURL(String className) {
    try {
      StringBuilder url = new StringBuilder();
      url.append(rootPath);
      url.append(File.separator);
      url.append("src");
      url.append(File.separator);
      url.append("main");
      url.append(File.separator);
      url.append("java");
      if (!new File(url.toString()).exists()) return searchFile(rootPath, className);
      String[] strs = className.split("\\.");
      for (int i = 0; i < strs.length; i++) {
        url.append(File.separator);
        url.append(strs[i]);
      }
      url.append(".java");
      File file = new File(url.toString());
      if (file.exists()) {
        return new URL("file://" + url.toString());
      } else {
        if (className.contains("$")) {
          return new URL("file://" + url.toString().split("\\$")[0] + ".java");
        } else {
          String enclosingClassName = removeAnonymousInnerClass(className);
          if (enclosingClassName == null) return null;
          return classNameToURL(enclosingClassName);
        }
      }
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  protected URL searchFile(String folder, String className) {
    String path = className.replace(".", File.separator) + ".java";
    List<URL> ret = new ArrayList<>();
    try (Stream<Path> walkStream = Files.walk(Paths.get(folder))) {
      walkStream
          .filter(p -> p.toFile().isFile())
          .forEach(
              f -> {
                if (f.toString().endsWith(path))
                  try {
                    ret.add(new URL("file://" + f));
                  } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                  }
              });
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret.isEmpty()) return null;
    return ret.get(0);
  }

  protected String removeAnonymousInnerClass(String className) {
    StringBuilder str = new StringBuilder();
    String[] strs = className.split("\\.");
    for (int i = 0; i < strs.length; i++) {
      String s = strs[i];
      if (i != strs.length - 1) {
        str.append(s);
        str.append(".");
      }
    }
    String fileName = str.toString();
    if (fileName.length() > 1) fileName = fileName.substring(0, fileName.length() - 1);
    else return null;
    return fileName;
  }

  @Override
  public String source() {
    return "TaintBench";
  }

  protected void searchInputFiles(String rootPath) {
    File rootFile = new File(rootPath);
    for (File f : rootFile.listFiles()) {
      if (!f.getName().endsWith(".json") && !f.getName().endsWith(".xml")) continue;
      else {
        if (f.getName().endsWith(".json") && InputValidation.isTAFformat(f)) {
          if (groundTruthPath != null) {
            Logger.log(
                TaintAnalysisResult.class.getName(),
                "There are multiple JSON files with TAF-format in "
                    + rootPath
                    + ". Please only keep one.");
            continue;
          } else groundTruthPath = rootFile.getAbsolutePath() + File.separator + f.getName();
        }
        if (f.getName().endsWith(".xml"))
          if (InputValidation.isAQLformat(f)) {
            if (aqlResultPath != null) {
              Logger.log(
                  TaintAnalysisResult.class.getName(),
                  "There are multiple XML files with AQLResult-format in "
                      + rootPath
                      + ". Please only keep one.");
              continue;
            } else aqlResultPath = rootFile.getAbsolutePath() + File.separator + f.getName();
          } else if (InputValidation.isFlowDroidFormat(f)) {
            if (fdResultPath != null) {
              Logger.log(
                  TaintAnalysisResult.class.getName(),
                  "There are multiple XML files with FlowDroid-Result-format in "
                      + rootPath
                      + ". Please only keep one.");
            } else {
              fdResultPath = rootFile.getAbsolutePath() + File.separator + f.getName();
            }
          }
      }
    }
  }

  @Override
  public void analyze(
      Collection<? extends Module> files, AnalysisConsumer consumer, boolean rerun) {
    if (rerun) {
      MagpieServer server = (MagpieServer) consumer;
      if (this.server == null) this.server = (TaintLanguageServer) server;
      JavaProjectService ps = (JavaProjectService) server.getProjectService("java").get();
      if (ps.getRootPath().isPresent()) {
        this.rootPath = ps.getRootPath().get().toString();
        if (groundTruth.isEmpty()) searchInputFiles(this.rootPath);
        if (this.groundTruthPath == null) {
          server
              .getClient()
              .showMessage(
                  new MessageParams(
                      MessageType.Info,
                      "TaintBench couldn't find JSON file in TAF-format of the ground truth in the project root."));
        } else {
          if (groundTruth.isEmpty() && this.groundTruthPath != null) {
            readGroundTruth();
          }
          if (aqlResults.isEmpty() && this.aqlResultPath != null) {
            loadAqlResults();
            readAqlResults();
          }

          if (fdResultPath != null) {
            try {
              this.loadedAqlResults =
                  FlowDroidResultParser.convertToAQL(
                      FlowDroidResultParser.readResultsWithPath(fdResultPath));
            } catch (XMLStreamException | IOException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
            readAqlResults();
          }

          Collection<AnalysisResult> allResults = new ArrayList<>();
          allResults.addAll(groundTruth);
          allResults.addAll(aqlResults);
          this.server.consume(allResults, source());
          this.server.consumeGroundTruth();
          this.server.consumeAQLResults();
          this.server.consumeMatchedResults(this.matchedResults);
        }
      }
    }
  }

  protected SourceCodePosition getPosition(URL url, String method, String statement) {
    SourceCodePosition pos = null;
    return pos;
  }
}
