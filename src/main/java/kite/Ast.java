package kite;

import java.util.List;

final class Ast {
    record Program(List<TypeDecl> types) {
    }

    record TypeDecl(String name, List<Member> members) {
    }

    sealed interface Member permits FieldDecl, MethodDecl {
    }

    record FieldDecl(String type, String name) implements Member {
    }

    record MethodDecl(String returnType, String name, List<Param> params, List<Stmt> body) implements Member {
    }

    record Param(String type, String name) {
    }

    sealed interface Stmt permits VarDecl, ExprStmt, ReturnStmt, IfStmt, WhileStmt {
    }

    record VarDecl(String type, String name, Expr initializer) implements Stmt {
    }

    record ExprStmt(Expr expr) implements Stmt {
    }

    record ReturnStmt(Expr value) implements Stmt {
    }

    record IfStmt(Expr condition, List<Stmt> thenBranch, List<Stmt> elseBranch) implements Stmt {
    }

    record WhileStmt(Expr condition, List<Stmt> body) implements Stmt {
    }

    sealed interface Expr permits Assign, Binary, Call, Get, Literal, Variable {
    }

    record Assign(Expr target, Expr value) implements Expr {
    }

    record Binary(Expr left, String operator, Expr right) implements Expr {
    }

    record Call(Expr callee, List<Expr> args) implements Expr {
    }

    record Get(Expr object, String name) implements Expr {
    }

    record Literal(String value) implements Expr {
    }

    record Variable(String name) implements Expr {
    }
}
