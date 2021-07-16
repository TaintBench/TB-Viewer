package de.upb.swt.tbviewer;

import com.google.gson.JsonObject;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.util.collections.Pair;
import java.util.Optional;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.Kind;
import org.eclipse.lsp4j.DiagnosticSeverity;

/** @author Linghui Luo */
public class TaintAnalysisResult implements AnalysisResult {
  private final Kind kind;
  private final Position position;
  private final String message;
  private final Iterable<Pair<Position, String>> related;
  private final DiagnosticSeverity severity;
  private final Pair<Position, String> repair;
  private final String code;
  private final Optional<JsonObject> finding;

  public TaintAnalysisResult(
      Kind kind,
      Position pos,
      String msg,
      Iterable<Pair<Position, String>> relatedInfo,
      DiagnosticSeverity severity,
      Pair<Position, String> repair,
      String code) {
    this(Optional.empty(), kind, pos, msg, relatedInfo, severity, repair, code);
  }

  public TaintAnalysisResult(
      Optional<JsonObject> finding,
      Kind kind,
      Position pos,
      String msg,
      Iterable<Pair<Position, String>> relatedInfo,
      DiagnosticSeverity severity,
      Pair<Position, String> repair,
      String code) {
    this.finding = finding;
    this.kind = kind;
    this.position = pos;
    this.message = msg;
    this.related = relatedInfo;
    this.severity = severity;
    this.repair = repair;
    this.code = code;
  }

  public Kind kind() {
    return this.kind;
  }

  public Position position() {
    return position;
  }

  public Iterable<Pair<Position, String>> related() {
    return related;
  }

  public DiagnosticSeverity severity() {
    return severity;
  }

  public Pair<Position, String> repair() {
    return repair;
  }

  public String toString(boolean useMarkdown) {
    return message;
  }

  @Override
  public String toString() {
    return "DataFlowResult [kind="
        + kind
        + ", position="
        + position
        + ", code="
        + code
        + ", message="
        + message
        + ", related="
        + related
        + ", severity="
        + severity
        + ", repair="
        + repair
        + "]";
  }

  public String code() {
    return code;
  }

  public TaintAnalysisResult copy(String msg) {
    return new TaintAnalysisResult(kind, position, msg, related, severity, repair, code);
  }

  public Optional<JsonObject> getFinding() {
    return this.finding;
  }
}
