package kite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import kite.Ast.Assign;
import kite.Ast.Binary;
import kite.Ast.Call;
import kite.Ast.Expr;
import kite.Ast.ExprStmt;
import kite.Ast.FieldDecl;
import kite.Ast.ForStmt;
import kite.Ast.Get;
import kite.Ast.IfStmt;
import kite.Ast.Literal;
import kite.Ast.Member;
import kite.Ast.MethodDecl;
import kite.Ast.Program;
import kite.Ast.ReturnStmt;
import kite.Ast.Stmt;
import kite.Ast.TypeDecl;
import kite.Ast.VarDecl;
import kite.Ast.Variable;
import kite.Ast.WhileStmt;

final class CGenerator {
    private final StringBuilder out = new StringBuilder();
    private final Map<String, Set<String>> fieldsByType = new HashMap<>();
    private Set<String> currentFields = Set.of();
    private Set<String> locals = Set.of();
    private int indentLevel;

    String generate(Program program) {
        out.append("#include <stdint.h>\n");
        out.append("#include <stdbool.h>\n");
        out.append("#include <stdio.h>\n\n");
        out.append("static void console_write(const char* text) { printf(\"%s\", text); }\n\n");

        indexFields(program);

        for (TypeDecl type : program.types()) {
            emitStruct(type);
        }

        for (TypeDecl type : program.types()) {
            currentFields = fieldsByType.getOrDefault(type.name(), Set.of());
            for (Member member : type.members()) {
                if (member instanceof MethodDecl method) {
                    emitMethod(type, method);
                }
            }
            currentFields = Set.of();
        }

        return out.toString();
    }

    private void indexFields(Program program) {
        for (TypeDecl type : program.types()) {
            Set<String> fields = new HashSet<>();
            for (Member member : type.members()) {
                if (member instanceof FieldDecl field) {
                    fields.add(field.name());
                }
            }
            fieldsByType.put(type.name(), fields);
        }
    }

    private void emitStruct(TypeDecl type) {
        out.append("typedef struct ").append(cStructName(type.name())).append(" {\n");
        for (Member member : type.members()) {
            if (member instanceof FieldDecl field) {
                out.append("    ").append(cType(field.type())).append(" ").append(field.name()).append(";\n");
            }
        }
        out.append("} ").append(cStructName(type.name())).append(";\n\n");
    }

    private void emitMethod(TypeDecl type, MethodDecl method) {
        boolean isEntrypoint = type.name().equals("main") && method.name().equals("main") && method.params().isEmpty();
        locals = new HashSet<>();
        method.params().forEach(param -> locals.add(param.name()));

        if (isEntrypoint) {
            out.append("int main(void)");
        } else {
            out.append(cType(method.returnType())).append(" ");
            out.append(type.name()).append("_").append(method.name()).append("(");
            List<String> params = new ArrayList<>();
            params.add(cStructName(type.name()) + "* self");
            params.addAll(method.params().stream()
                    .map(param -> cType(param.type()) + " " + param.name())
                    .toList());
            out.append(String.join(", ", params));
            out.append(")");
        }

        out.append(" {\n");
        indentLevel++;
        for (Stmt stmt : method.body()) {
            emitStmt(stmt);
        }
        if (isEntrypoint) {
            line("return 0;");
        }
        indentLevel--;
        out.append("}\n\n");
        locals = Set.of();
    }

    private void emitStmt(Stmt stmt) {
        if (stmt instanceof VarDecl varDecl) {
            locals.add(varDecl.name());
            indent();
            out.append(cType(varDecl.type())).append(" ").append(varDecl.name());
            if (varDecl.initializer() != null) {
                out.append(" = ").append(expr(varDecl.initializer()));
            }
            out.append(";\n");
        } else if (stmt instanceof ExprStmt exprStmt) {
            line(expr(exprStmt.expr()) + ";");
        } else if (stmt instanceof ReturnStmt returnStmt) {
            indent();
            out.append("return");
            if (returnStmt.value() != null) {
                out.append(" ").append(expr(returnStmt.value()));
            }
            out.append(";\n");
        } else if (stmt instanceof IfStmt ifStmt) {
            line("if (" + expr(ifStmt.condition()) + ") {");
            emitBlock(ifStmt.thenBranch());
            if (ifStmt.elseBranch().isEmpty()) {
                line("}");
            } else {
                line("} else {");
                emitBlock(ifStmt.elseBranch());
                line("}");
            }
        } else if (stmt instanceof WhileStmt whileStmt) {
            line("while (" + expr(whileStmt.condition()) + ") {");
            emitBlock(whileStmt.body());
            line("}");
        } else if (stmt instanceof ForStmt forStmt) {
            line("for (" + forInitializer(forStmt.initializer()) + "; " + optionalExpr(forStmt.condition()) + "; "
                    + optionalExpr(forStmt.increment()) + ") {");
            emitBlock(forStmt.body());
            line("}");
        }
    }

    private String forInitializer(Stmt initializer) {
        if (initializer == null) {
            return "";
        }
        if (initializer instanceof VarDecl varDecl) {
            locals.add(varDecl.name());
            String text = cType(varDecl.type()) + " " + varDecl.name();
            if (varDecl.initializer() != null) {
                text += " = " + expr(varDecl.initializer());
            }
            return text;
        }
        if (initializer instanceof ExprStmt exprStmt) {
            return expr(exprStmt.expr());
        }
        throw new IllegalStateException("Unsupported for initializer: " + initializer);
    }

    private String optionalExpr(Expr expr) {
        if (expr == null) {
            return "";
        }
        return expr(expr);
    }

    private void emitBlock(List<Stmt> body) {
        indentLevel++;
        for (Stmt stmt : body) {
            emitStmt(stmt);
        }
        indentLevel--;
    }

    private void line(String text) {
        indent();
        out.append(text).append("\n");
    }

    private void indent() {
        out.append("    ".repeat(indentLevel));
    }

    private String expr(Expr expr) {
        if (expr instanceof Literal literal) {
            return literal.value();
        }
        if (expr instanceof Variable variable) {
            if (currentFields.contains(variable.name()) && !locals.contains(variable.name())) {
                return "self->" + variable.name();
            }
            return variable.name();
        }
        if (expr instanceof Get get) {
            if (get.object() instanceof Variable variable && variable.name().equals("console") && get.name().equals("write")) {
                return "console_write";
            }
            return expr(get.object()) + "." + get.name();
        }
        if (expr instanceof Call call) {
            String args = call.args().stream().map(this::expr).collect(Collectors.joining(", "));
            return expr(call.callee()) + "(" + args + ")";
        }
        if (expr instanceof Assign assign) {
            return expr(assign.target()) + " = " + expr(assign.value());
        }
        if (expr instanceof Binary binary) {
            return expr(binary.left()) + " " + binary.operator() + " " + expr(binary.right());
        }
        throw new IllegalStateException("Unknown expression: " + expr);
    }

    private String cType(String kiteType) {
        return switch (kiteType) {
            case "void" -> "void";
            case "int" -> "int32_t";
            case "uint" -> "uint32_t";
            case "long" -> "int64_t";
            case "ulong" -> "uint64_t";
            case "float" -> "float";
            case "double" -> "double";
            case "byte" -> "uint8_t";
            case "char" -> "char";
            case "bool" -> "bool";
            case "string" -> "const char*";
            default -> cStructName(kiteType);
        };
    }

    private String cStructName(String kiteType) {
        return "kite_" + kiteType;
    }
}
