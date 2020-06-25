package de.upb.swt.tbviewer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.ibm.wala.util.collections.Pair;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

/** @author Linghui Luo */
public class SourceCodePositionFinder {

  /**
   * read Java file and compare each line to statement.
   *
   * @param javaFile
   * @param statement
   * @return
   */
  public static Range searchStatementInFile(File javaFile, String statement) {
    try {
      BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(new FileInputStream(javaFile), StandardCharsets.UTF_8));
      String line;
      int i = 0;
      while ((line = reader.readLine()) != null) {
        i++;
        if (line.trim().equals(statement)) {
          int column = 0;
          line = line.split("//")[0];
          for (char c : line.toCharArray()) {

            if (c != ' ') {
              break;
            }
            column++;
          }
          Range range =
              new Range(new Position(i, column), new Position(i, column + statement.length()));
          return range;
        }
      }

    } catch (IOException e) {
      Logger.log(
          SourceCodePositionFinder.class.getName(), "IOException for " + javaFile.toString());
    }
    return null;
  }

  /**
   * return the SourceCodePosition at given line number.
   *
   * @param url
   * @param statement
   * @param lineNo
   * @return
   */
  public static SourceCodePosition getFromLineNo(URL url, String statement, int lineNo) {
    try {
      File javaFile = new File(url.getPath());
      BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(new FileInputStream(javaFile), StandardCharsets.UTF_8));
      String line;
      int i = 0;
      while ((line = reader.readLine()) != null) {
        i++;
        if (i == lineNo) {
          int column = 0;
          line = line.split("//")[0];
          for (char c : line.toCharArray()) {

            if (c != ' ') {
              break;
            }
            column++;
          }
          Range range =
              new Range(new Position(i, column), new Position(i, column + statement.length()));
          SourceCodePosition pos =
              new SourceCodePosition(
                  url, range.begin.line, range.begin.column, range.end.line, range.end.column);
          return pos;
        }
      }

    } catch (IOException e) {
      Logger.log(SourceCodePositionFinder.class.getName(), "IOException for " + url.toString());
    }
    return null;
  }

  public static SourceCodePosition get(URL url, String method, String statement) {
    File javaFile = new File(url.getPath());
    JavaParser parser = new JavaParser();
    Optional<CompilationUnit> result;
    try {
      result = parser.parse(javaFile).getResult();
      if (result.isPresent()) {
        CompilationUnit cu = result.get();
        MethodVisitor visitor = new MethodVisitor(method, statement);
        visitor.visit(cu, null);
        Range range = visitor.getRange();
        if (range == null) range = searchStatementInFile(javaFile, statement);
        if (range != null) {
          return new SourceCodePosition(
              url, range.begin.line, range.begin.column, range.end.line, range.end.column);
        } else {
          String output =
              "can't find source code position for \n"
                  + url.toString()
                  + "\n METHOD\t"
                  + method
                  + "\n STMT\t"
                  + statement
                  + "\n";
          Logger.log(SourceCodePositionFinder.class.getName(), output);
        }
      }
    } catch (FileNotFoundException e) {
      Logger.log(
          SourceCodePositionFinder.class.getName(),
          "FileNotFoundException for " + javaFile.toString());
    }
    return null;
  }

  public static Set<Pair<SourceCodePosition, String>> getWithRegex(
      URL url, String method, String regex, List<String> constantParams) {
    File javaFile = new File(url.getPath());
    JavaParser parser = new JavaParser();
    Optional<CompilationUnit> result;
    Set<Pair<SourceCodePosition, String>> ret = new HashSet<>();
    try {
      result = parser.parse(javaFile).getResult();
      if (result.isPresent()) {
        CompilationUnit cu = result.get();
        MethodVisitor visitor = new MethodVisitor(method, regex, constantParams, true);
        visitor.visit(cu, null);
        Map<Range, String> possibleLocations = visitor.getPossibleLocations();
        if (!possibleLocations.isEmpty()) {
          for (Entry<Range, String> entry : possibleLocations.entrySet()) {
            Range range = entry.getKey();

            String sourceCode = entry.getValue();
            if (sourceCode.equals("uncertain")) {
              sourceCode = sourceCode + " " + regex;
              int startLine =
                  magpiebridge.util.SourceCodePositionFinder.findCode(javaFile, range.begin.line)
                      .range
                      .getStart()
                      .getLine();
              int startColumn =
                  magpiebridge.util.SourceCodePositionFinder.findCode(javaFile, range.begin.line)
                      .range
                      .getStart()
                      .getCharacter();
              int endLine =
                  magpiebridge.util.SourceCodePositionFinder.findCode(javaFile, range.begin.line)
                      .range
                      .getEnd()
                      .getLine();
              int endColumn =
                  magpiebridge.util.SourceCodePositionFinder.findCode(javaFile, range.begin.line)
                      .range
                      .getEnd()
                      .getCharacter();
              range =
                  new Range(
                      new Position(startLine + 1, startColumn),
                      new Position(endLine + 1, endColumn));
            }
            SourceCodePosition pos =
                new SourceCodePosition(
                    url, range.begin.line, range.begin.column, range.end.line, range.end.column);
            Pair<SourceCodePosition, String> codeInfo = Pair.make(pos, sourceCode);
            ret.add(codeInfo);
          }
        } else {
          Logger.log(
              SourceCodePositionFinder.class.getName(),
              "can't find source code position for \n"
                  + url.toString()
                  + "\n METHOD\t"
                  + method
                  + "\n REGEX_STMT\t"
                  + regex
                  + "\n");
        }
      }
    } catch (FileNotFoundException e) {
      Logger.log(
          SourceCodePositionFinder.class.getName(),
          "FileNotFoundException for " + javaFile.toString());
    }
    return ret;
  }
}
