package de.upb.swt.tbviewer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @author Linghui Luo */
public class Location {

  private final LocationKind kind;
  private String classSignature;

  public LocationKind getKind() {
    return kind;
  }

  public String getClassSignature() {
    return classSignature;
  }

  public String getMethodSignature() {
    return methodSignature;
  }

  public String getStatement() {
    return statement;
  }

  public int getLinenumber() {
    return linenumber;
  }

  private String methodSignature;
  private String statement;
  private int linenumber;

  private static Pattern javaMethodPattern = createJavaMethodPattern();
  private static Pattern jimpleMethodPattern = createJimpleMethodPattern();

  public static Pattern createJavaMethodPattern() {
    // non capture modifier
    String group0 =
        "(?:public|private|protected|static|final|native|synchronized|abstract|transient)";
    String group1 = "(.+)"; // return type
    String group2 = "(.+)"; // method name
    String group3 = "(.*?)"; // parameters
    StringBuilder sb = new StringBuilder();
    sb.append(group0);
    sb.append("\\s+");
    sb.append(group1);
    sb.append("\\s+");
    sb.append(group2);
    sb.append("\\(");
    sb.append(group3);
    sb.append("\\)");
    String regex = sb.toString();
    Pattern p = Pattern.compile(regex);
    return p;
  }

  public static Pattern createJimpleMethodPattern() {
    // non capture class signature
    String group0 = "(?:.+)";
    String group1 = "(.+)"; // return type
    String group2 = "(.+)"; // method name
    String group3 = "(.*?)"; // parameter types
    StringBuilder sb = new StringBuilder();
    sb.append(group0);
    sb.append("\\s+");
    sb.append(group1);
    sb.append("\\s+");
    sb.append(group2);
    sb.append("\\(");
    sb.append(group3);
    sb.append("\\)");
    String regex = sb.toString();
    Pattern p = Pattern.compile(regex);
    return p;
  }

  public static boolean compareMethod(String javaMethod, String jimpleMethod) {
    Matcher javaMatcher = javaMethodPattern.matcher(javaMethod);
    Matcher jimpleMatcher = jimpleMethodPattern.matcher(jimpleMethod);
    if (javaMatcher.find()) {
      String javaReturnType = javaMatcher.group(1);
      if (javaReturnType.contains(" ")) {
        String[] strs = javaReturnType.split(" ");
        javaReturnType = strs[strs.length - 1];
      }
      String javaMethodName = javaMatcher.group(2);
      String javaParameterString = javaMatcher.group(3);
      // replace all types between < > for generic types
      javaParameterString = javaParameterString.replaceAll("\\<.+\\>", "");
      String[] javaParameters = javaParameterString.split(",");

      if (jimpleMatcher.find()) {
        String jimpleReturenType = jimpleMatcher.group(1);
        String jimpleMethodName = jimpleMatcher.group(2);
        String[] jimpleParameterTypes = jimpleMatcher.group(3).split(",");
        if (jimpleReturenType.endsWith(javaReturnType)) {
          if (jimpleMethodName.equals(javaMethodName)) {
            if (javaParameters.length == jimpleParameterTypes.length) {
              boolean paraMatch = true;
              for (int i = 0; i < javaParameters.length; i++) {
                String javaPara = javaParameters[i].split("\\s")[0];
                String jimplePara = jimpleParameterTypes[i];
                if (javaPara.endsWith("...")) // take care of varargs
                {
                  javaPara = javaPara.replace("...", "[]");
                }
                if (!jimplePara.endsWith(javaPara)) {
                  paraMatch = false;
                  break;
                }
              }
              return paraMatch;
            }
          }
        }

      } else {
        // TODO: consider constructors!
        Logger.log(
            Location.class.getName(), jimpleMethod + " does not match the jimple method pattern");
      }
    } else {
      if (jimpleMatcher.find()) {
        return false;
      }
      Logger.log(Location.class.getName(), javaMethod + " does not match the java method pattern");
    }
    return false;
  }

  public static boolean compareStatements(String javaStatement, String jimpleStatement) {
    return false;
  }

  public Location(
      LocationKind kind,
      String classSignature,
      String methodSignature,
      String statement,
      int linenumber) {
    super();
    this.kind = kind;
    this.classSignature = classSignature;
    this.methodSignature = methodSignature;
    this.statement = statement;
    this.linenumber = linenumber;
  }

  public Location(String method, String statement, int linenumber) {
    this.kind = LocationKind.Jimple;
    this.statement = statement;
    String[] splits = method.split(":");
    this.classSignature = splits[0].replace("<", "").trim();
    this.methodSignature = method.replace("<", "").replace(">", "");
    this.linenumber = linenumber;
  }

  public static boolean maybeEqual(Location a, Location b, boolean compareStatements) {
    if (a.kind == b.kind) {
      return a.classSignature.equals(b.classSignature)
          && a.methodSignature.equals(b.methodSignature)
          && a.statement.equals(b.statement);
    } else {
      if (a.kind.equals(LocationKind.Java) && b.kind.equals(LocationKind.Jimple)) {
        String aClass = a.classSignature;
        String bClass = b.classSignature;
        if (bClass.contains("$")) {
          if (Character.isDigit(bClass.charAt(bClass.length() - 1)))
            bClass = bClass.replace("$", ".AnonymousClass"); // handle anonymous Class
          else bClass = bClass.replace("$", "."); // handle inner class
        }

        if (aClass.equals(bClass)) {
          if (compareMethod(a.methodSignature, b.methodSignature))
            if (a.linenumber == b.linenumber && a.linenumber != -1) {
              return true;
            } else {
              if (!compareStatements) return true;
              else return compareStatements(a.statement, b.statement);
            }
        }
      } else if (a.kind.equals(LocationKind.Jimple) && b.kind.equals(LocationKind.Java)) {
        return maybeEqual(b, a, compareStatements);
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "Location [kind="
        + kind
        + ", classSignature="
        + classSignature
        + ", methodSignature="
        + methodSignature
        + ", statement="
        + statement
        + ", linenumber="
        + linenumber
        + "]";
  }
}
