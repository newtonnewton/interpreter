package com.craftinginterpreters.lox;

//> Functions import-array-list
import java.util.ArrayList;
//< Functions import-array-list
//> Resolving and Binding import-hash-map
import java.util.HashMap;
//< Resolving and Binding import-hash-map
import java.util.List;
//< Statements and State import-list
//> Resolving and Binding import-map
import java.util.Map;
//< Resolving and Binding import-map

import com.craftinginterpreters.lox.Expr.Assign;
import com.craftinginterpreters.lox.Expr.Call;
import com.craftinginterpreters.lox.Expr.Get;
import com.craftinginterpreters.lox.Expr.Logical;
import com.craftinginterpreters.lox.Expr.Set;
import com.craftinginterpreters.lox.Expr.Super;
import com.craftinginterpreters.lox.Expr.This;
import com.craftinginterpreters.lox.Expr.Variable;
import com.craftinginterpreters.lox.Stmt.Block;
import com.craftinginterpreters.lox.Stmt.Class;
import com.craftinginterpreters.lox.Stmt.Function;
import com.craftinginterpreters.lox.Stmt.If;
import com.craftinginterpreters.lox.Stmt.Return;
import com.craftinginterpreters.lox.Stmt.Var;
import com.craftinginterpreters.lox.Stmt.While;

class Interpreter implements Expr.Visitor<Object>,
                             Stmt.Visitor<Void>{
	
final Environment globals = new Environment();
private Environment environment = globals;

Interpreter() {
    globals.define("clock", new LoxCallable() {
      @Override
      public int arity() { return 0; }

      @Override
      public Object call(Interpreter interpreter,
                         List<Object> arguments) {
        return (double)System.currentTimeMillis() / 1000.0;
      }

      @Override
      public String toString() { return "<native fn>"; }
    });
}
	
@Override
public Void visitExpressionStmt(Stmt.Expression stmt) {
  evaluate(stmt.expression);
  return null;
}

@Override
public Void visitPrintStmt(Stmt.Print stmt) {
  Object value = evaluate(stmt.expression);
  System.out.println(stringify(value));
  return null;
}

void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
}

private void execute(Stmt stmt) {
    stmt.accept(this);
}
    
@Override
public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
}

@Override
public Object visitGroupingExpr(Expr.Grouping expr) {
  return evaluate(expr.expression);
}

private Object evaluate(Expr expr) {
  return expr.accept(this);
}

@Override
public Object visitUnaryExpr(Expr.Unary expr) {
  Object right = evaluate(expr.right);

  switch (expr.operator.type) {
    case BANG:
      return !isTruthy(right);
    case MINUS:
      checkNumberOperand(expr.operator, right);
      return -(double)right;
  }

  // Unreachable.
  return null;
}

private boolean isTruthy(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean)object;
    return true;
}

@Override
public Object visitBinaryExpr(Expr.Binary expr) {
  Object left = evaluate(expr.left);
  Object right = evaluate(expr.right); // [left]

  switch (expr.operator.type) {
//> binary-equality
    case BANG_EQUAL: return !isEqual(left, right);
    case EQUAL_EQUAL: return isEqual(left, right);
//< binary-equality
//> binary-comparison
    case GREATER:
//> check-greater-operand
      checkNumberOperands(expr.operator, left, right);
//< check-greater-operand
      return (double)left > (double)right;
    case GREATER_EQUAL:
//> check-greater-equal-operand
      checkNumberOperands(expr.operator, left, right);
//< check-greater-equal-operand
      return (double)left >= (double)right;
    case LESS:
//> check-less-operand
      checkNumberOperands(expr.operator, left, right);
//< check-less-operand
      return (double)left < (double)right;
    case LESS_EQUAL:
//> check-less-equal-operand
      checkNumberOperands(expr.operator, left, right);
//< check-less-equal-operand
      return (double)left <= (double)right;
//< binary-comparison
    case MINUS:
//> check-minus-operand
      checkNumberOperands(expr.operator, left, right);
//< check-minus-operand
      return (double)left - (double)right;
//> binary-plus
    case PLUS:
      if (left instanceof Double && right instanceof Double) {
        return (double)left + (double)right;
      } // [plus]

      if (left instanceof String && right instanceof String) {
        return (String)left + (String)right;
      }

/* Evaluating Expressions binary-plus < Evaluating Expressions string-wrong-type
      break;
*/
//> string-wrong-type
      throw new RuntimeError(expr.operator,
          "Operands must be two numbers or two strings.");
//< string-wrong-type
//< binary-plus
    case SLASH:
//> check-slash-operand
      checkNumberOperands(expr.operator, left, right);
//< check-slash-operand
      return (double)left / (double)right;
    case STAR:
//> check-star-operand
      checkNumberOperands(expr.operator, left, right);
//< check-star-operand
      return (double)left * (double)right;
  }

  // Unreachable.
  return null;
}

private boolean isEqual(Object a, Object b) {
    if (a == null && b == null) return true;
    if (a == null) return false;

    return a.equals(b);
}

private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be a number.");
}

private void checkNumberOperands(Token operator,
        Object left, Object right) {
if (left instanceof Double && right instanceof Double) return;

throw new RuntimeError(operator, "Operands must be numbers.");
}

void interpret(Expr expression) { 
    try {
      Object value = evaluate(expression);
      System.out.println(stringify(value));
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

private String stringify(Object object) {
    if (object == null) return "nil";

    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    return object.toString();
  }

@Override
public Void visitBlockStmt(Stmt.Block stmt) {
  executeBlock(stmt.statements, new Environment(environment));
  return null;
}

void executeBlock(List<Stmt> statements,
        Environment environment) {
  Environment previous = this.environment;
  try {
    this.environment = environment;

    for (Stmt statement : statements) {
      execute(statement);
    }
  } finally {
    this.environment = previous;
  }
}

@Override
public Void visitClassStmt(Class stmt) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Void visitFunctionStmt(Function stmt) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Void visitIfStmt(Stmt.If stmt) {
  if (isTruthy(evaluate(stmt.condition))) {
    execute(stmt.thenBranch);
  } else if (stmt.elseBranch != null) {
    execute(stmt.elseBranch);
  }
  return null;
}

@Override
public Void visitReturnStmt(Return stmt) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Void visitVarStmt(Stmt.Var stmt) {
  Object value = null;
  if (stmt.initializer != null) {
    value = evaluate(stmt.initializer);
  }

  environment.define(stmt.name.lexeme, value);
  return null;
}

@Override
public Void visitWhileStmt(Stmt.While stmt) {
  while (isTruthy(evaluate(stmt.condition))) {
    execute(stmt.body);
  }
  return null;
}


@Override
public Object visitAssignExpr(Expr.Assign expr) {
  Object value = evaluate(expr.value);
  environment.assign(expr.name, value);
  return value;
}

@Override
public Object visitCallExpr(Expr.Call expr) {
  Object callee = evaluate(expr.callee);

  List<Object> arguments = new ArrayList<>();
  for (Expr argument : expr.arguments) { 
    arguments.add(evaluate(argument));
  }
  
//> check-is-callable
  if (!(callee instanceof LoxCallable)) {
    throw new RuntimeError(expr.paren,
        "Can only call functions and classes.");
  }
  
  LoxCallable function = (LoxCallable)callee;
  if (arguments.size() != function.arity()) {
      throw new RuntimeError(expr.paren, "Expected " +
          function.arity() + " arguments but got " +
          arguments.size() + ".");
  }
  return function.call(this, arguments);
}

@Override
public Object visitGetExpr(Get expr) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Object visitLogicalExpr(Expr.Logical expr) {
  Object left = evaluate(expr.left);

  if (expr.operator.type == TokenType.OR) {
    if (isTruthy(left)) return left;
  } else {
    if (!isTruthy(left)) return left;
  }

  return evaluate(expr.right);
}

@Override
public Object visitSetExpr(Set expr) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Object visitSuperExpr(Super expr) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Object visitThisExpr(This expr) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Object visitVariableExpr(Expr.Variable expr) {
  return environment.get(expr.name);
}



}
