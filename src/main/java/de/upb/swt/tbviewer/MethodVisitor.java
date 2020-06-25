package de.upb.swt.tbviewer;

import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithBody;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** @author Linghui Luo */
public class MethodVisitor extends VoidVisitorAdapter<Object> {

  private String targetMethod;
  private String targetStatement;

  private boolean testJimple; // if the statement is in jimple format
  private String targetCallName;
  private boolean isConstructor = false;
  private List<String> constantParams;

  private Range range;
  private String sourceCode;

  private Map<Range, String> possibleLocations; // this is used when testJimple is true

  private Range methodBegin;

  /**
   * Search the location of given source code.
   *
   * @param targetMethod
   * @param targetStatement
   */
  public MethodVisitor(String targetMethod, String targetStatement) {
    this(targetMethod, targetStatement, Collections.EMPTY_LIST, false);
    this.sourceCode = targetStatement;
  }

  /**
   * Search possible lines of source code which match the given jimple statement.
   *
   * @param targetMethod
   * @param targetGenericJimpleStatement
   * @param constantParams
   * @param testRegex
   */
  public MethodVisitor(
      String targetMethod,
      String targetGenericJimpleStatement,
      List<String> constantParams,
      boolean testRegex) {
    this.targetMethod = targetMethod;
    this.targetStatement = targetGenericJimpleStatement;
    this.testJimple = testRegex;
    if (this.testJimple) {
      String[] splits = this.targetStatement.split(" ");
      if (splits.length > 0) {
        String[] sps = splits[splits.length - 1].split("\\(");
        if (sps.length > 0) {
          this.targetCallName = sps[0];
          if (this.targetCallName.equals("<init>")) {
            String cName = splits[0].replace(":", "");
            String[] ps = cName.split("\\.");
            if (ps.length > 0) {
              this.targetCallName = ps[ps.length - 1];
              isConstructor = true;
            }
          }
        }
      }
    }
    this.constantParams = constantParams;
    this.possibleLocations = new HashMap<Range, String>();
  }

  public Range setRange(Range goal, Range r) {
    if (goal == null) return r;
    else return goal;
  }

  private boolean checkForJimple(String stmt, String method) {
    String compareString = "." + targetCallName + "(";
    if (this.isConstructor) compareString = "new " + targetCallName + "(";
    if (stmt.contains(compareString)) {
      if (Location.compareMethod(method, targetMethod)) {
        if (this.constantParams.isEmpty()) return true;
        else {
          for (String constant : constantParams) {
            if (!stmt.contains(constant)) return false;
          }
          return true;
        }
      }
    }
    return false;
  }

  public void visit(Statement stmt, String method) {
    if (range != null) return;
    Range r = stmt.getRange().get();
    if (r.begin.line == r.end.line) {
      if (!testJimple) {
        String stmtstr = stmt.toString();
        if (stmtstr.equals(targetStatement) && method.equals(targetMethod)) {
          this.range = setRange(this.range, r);
        } else if (stmtstr.replaceAll("\\s", "").equals(targetStatement.replaceAll("\\s", ""))
            && method.equals(targetMethod)) {
          // try replace all white space
          this.range = setRange(this.range, r);
          this.possibleLocations.put(this.range, sourceCode);
        }
      } else {
        if (checkForJimple(stmt.toString(), method))
          if (!this.possibleLocations.containsKey(range)) {
            this.possibleLocations.put(r, stmt.toString());
          }
      }
    } else if (stmt instanceof SynchronizedStmt) {

      SynchronizedStmt synStmt = (SynchronizedStmt) stmt;
      for (Statement s : synStmt.getBody().getStatements()) {
        visit(s, method);
      }
    } else if (stmt instanceof BlockStmt) {
      BlockStmt block = (BlockStmt) stmt;
      for (Statement s : block.getStatements()) {
        visit(s, method);
      }
    } else {
      if (stmt instanceof ExpressionStmt) {
        ExpressionStmt c = (ExpressionStmt) stmt;
        if (c.getExpression() instanceof MethodCallExpr) {
          // declare anonymous class in a method call
          MethodCallExpr expr = (MethodCallExpr) c.getExpression();
          if (targetStatement.contains("new ")
              && expr.toString().startsWith(targetStatement)
              && method.equals(targetMethod)) {
            setRangeFrom(c, expr.toString());
          }
          for (Expression arg : expr.getArguments()) {
            if (arg instanceof ObjectCreationExpr) {
              ObjectCreationExpr o = (ObjectCreationExpr) arg;
              visit(o);
            }
          }

          for (Node child : expr.getChildNodes()) {
            if (child instanceof ObjectCreationExpr) {
              ObjectCreationExpr o = (ObjectCreationExpr) child;
              visit(o);
            }
          }
        } else if (c.getExpression() instanceof VariableDeclarationExpr) {
          // declare anonymous class in an assignment
          VariableDeclarationExpr expr = (VariableDeclarationExpr) c.getExpression();
          if (targetStatement.contains("new ")
              && expr.toString().startsWith(targetStatement)
              && method.equals(targetMethod)) {
            setRangeFrom(stmt, expr.toString());
          }
          for (VariableDeclarator v : expr.getVariables()) {
            for (Node child : v.getChildNodes()) {
              if (child instanceof ObjectCreationExpr) {
                ObjectCreationExpr o = (ObjectCreationExpr) child;
                visit(o);
              }
            }
          }
        }
      }

      if (stmt instanceof NodeWithBody<?>) {
        // DoStmt, ForEachStmt, ForStmt, WhileStmt
        if (stmt instanceof WhileStmt) {
          WhileStmt c = (WhileStmt) stmt;
          if (!testJimple) {
            if (targetStatement.contains("while") && method.equals(targetMethod))
              setRangeFrom(c, c.toString());
          } else {
            checkChildNodesForJimple(stmt, method);
          }

        } else if (stmt instanceof ForStmt) {
          ForStmt c = (ForStmt) stmt;
        } else if (stmt instanceof ForEachStmt) {
          ForEachStmt c = (ForEachStmt) stmt;
          if (!testJimple) {
            if (targetStatement.contains("for") && method.equals(targetMethod))
              setRangeFrom(c, c.toString());
          } else {
            checkChildNodesForJimple(stmt, method);
          }
        } else if (stmt instanceof DoStmt) {
          DoStmt c = (DoStmt) stmt;
          if (!testJimple) {
            if (targetStatement.contains("do") && method.equals(targetMethod))
              setRangeFrom(c, c.toString());
          } else {
            checkChildNodesForJimple(stmt, method);
          }
        }
        if (range == null) {
          NodeWithBody<?> node = (NodeWithBody<?>) stmt;
          Statement body = node.getBody();
          visit(body, method);
        }
      }

      if (stmt instanceof IfStmt) {
        IfStmt ifstmt = (IfStmt) stmt;
        if (!testJimple) {
          if (targetStatement.contains("if") && method.equals(targetMethod))
            setRangeFrom(ifstmt, ifstmt.getCondition().toString());
        } else checkChildNodesForJimple(stmt, method);
        Statement thenStmt = ifstmt.getThenStmt();
        if (thenStmt instanceof BlockStmt) visit((BlockStmt) thenStmt, method);
        else visit(thenStmt, method);
        if (ifstmt.getElseStmt().isPresent()) {
          Statement elseStmt = ifstmt.getElseStmt().get();
          if (elseStmt instanceof BlockStmt) visit((BlockStmt) elseStmt, method);
          else visit(elseStmt, method);
        }
      }
      if (stmt instanceof SwitchStmt) {
        SwitchStmt switchStmt = (SwitchStmt) stmt;
        if (!testJimple) {
          if (targetStatement.contains("switch") && method.equals(targetMethod)) {
            setRangeFrom(switchStmt, switchStmt.getChildNodes().get(0).toString());
          }
        } else checkChildNodesForJimple(stmt, method);
        for (SwitchEntry n : switchStmt.getEntries()) {
          for (Statement s : n.getStatements()) {
            visit(s, method);
          }
        }
      }
      if (stmt instanceof TryStmt) {
        TryStmt tryStmt = (TryStmt) stmt;
        visit(tryStmt.getTryBlock(), method);
        if (tryStmt.getFinallyBlock().isPresent()) {
          visit(tryStmt.getFinallyBlock().get(), method);
        }
      }
    }
  }

  private void checkChildNodesForJimple(Statement stmt, String method) {
    for (Node n : stmt.getChildNodes()) {
      if (n instanceof MethodCallExpr) {
        String callExpr = n.toString();
        if (checkForJimple(callExpr, method)) {
          Range range = n.getRange().get();
          if (!this.possibleLocations.containsKey(range)) {
            this.possibleLocations.put(range, callExpr);
          }
        }
      }
      if (n instanceof Expression) checkChildNodesForJimple((Expression) n, method);
    }
  }

  private void checkChildNodesForJimple(Expression expr, String method) {
    for (Node n : expr.getChildNodes()) {
      if (n instanceof MethodCallExpr) {
        String callExpr = n.toString();
        if (checkForJimple(callExpr, method)) {
          Range range = n.getRange().get();
          if (!this.possibleLocations.containsKey(range)) {
            this.possibleLocations.put(range, callExpr);
          }
        }
      }
    }
  }

  public void setRangeFrom(Statement c, String checkStr) {
    if (range != null) return;
    if (checkStr.contains(targetStatement) || targetStatement.contains(checkStr)) {
      Range range = c.getTokenRange().get().getBegin().getRange().get();
      range = range.withEndColumn(range.begin.column + targetStatement.length());
      this.range = setRange(this.range, range);
    }
  }

  public void visit(ConstructorDeclaration n, Object arg) {
    if (range != null) return;
    String declaration = n.getDeclarationAsString(true, false);
    String name = n.getNameAsString();
    BlockStmt op = n.getBody();
    visit(Optional.of(op), declaration, name);
  }

  public void visit(MethodDeclaration method, Object arg) {
    if (range != null) return;
    String declaration = method.getDeclarationAsString(true, false);
    String methodName = method.getNameAsString();
    Optional<BlockStmt> op = method.getBody();
    visit(op, declaration, methodName);
  }

  public void visit(Optional<BlockStmt> op, String declaration, String methodName) {
    // System.err.println(declaration);
    if (targetMethod.equals(declaration)
        || (this.testJimple == true && targetMethod.contains(methodName))) {
      if (op.isPresent()) {
        BlockStmt block = op.get();
        Optional<Range> range = block.getRange();
        if (range.isPresent()) {
          methodBegin = new Range(range.get().begin, range.get().begin);
          if (testJimple && targetStatement.contains(":= @parameter")) {
            // the Jimple statement refers to parameter of a method
            this.possibleLocations.put(methodBegin, declaration);
          }
        }
        NodeList<Statement> stmts = block.getStatements();
        for (Statement s : stmts) {
          visit(s, declaration);
        }
      }
    }
    if (range == null && op.isPresent()) {
      visit(op.get(), declaration);
    }
  }

  public void visit(ObjectCreationExpr o) {
    if (range != null) return;
    for (Expression n : o.getArguments()) {
      if (n instanceof ObjectCreationExpr) {
        visit((ObjectCreationExpr) n);
      }
    }
    if (o.getAnonymousClassBody().isPresent()) {
      for (BodyDeclaration<?> d : o.getAnonymousClassBody().get()) {
        if (d instanceof MethodDeclaration) {
          MethodDeclaration method = (MethodDeclaration) d;
          if (this.targetStatement.contains(method.getDeclarationAsString())) {
            // target statement is method declaration for anonymous class
            Range range = method.getTokenRange().get().getBegin().getRange().get();
            range = range.withEndColumn(range.begin.column + targetStatement.length());
            this.range = setRange(this.range, range);
            break;
          } else {
            // check if target statement is in the method body of anonymous class
            Optional<BlockStmt> op = method.getBody();
            if (op.isPresent()) {
              BlockStmt block = op.get();
              visit(block, method.getDeclarationAsString(true, false));
            }
          }
        }
      }
    }
  }

  public void visit(BlockStmt block, String method) {
    if (range != null) return;
    NodeList<Statement> stmts = block.getStatements();
    for (Statement s : stmts) {
      visit(s, method);
    }
  }

  public Range getRange() {
    if (!testJimple) return range;
    else {
      Logger.log(
          MethodVisitor.class.getName() + ".getRange()",
          "use this method only when given Java source code, currently given Jimple code.");
      return null;
    }
  }

  public Map<Range, String> getPossibleLocations() {
    if (possibleLocations.isEmpty() && methodBegin != null) {
      this.possibleLocations.put(methodBegin, "uncertain");
    }
    return this.possibleLocations;
  }

  public String getSourceCode() {
    if (!testJimple) {
      return sourceCode;
    } else {
      Logger.log(
          MethodVisitor.class.getName() + ".getSourceCode()",
          "use this method only when given Java source code, currently given Jimple code.");
      return null;
    }
  }
}
