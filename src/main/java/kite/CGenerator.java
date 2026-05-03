package kite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import kite.Ast.Assign;
import kite.Ast.ArrayNew;
import kite.Ast.Binary;
import kite.Ast.Call;
import kite.Ast.DeleteStmt;
import kite.Ast.Expr;
import kite.Ast.ExprStmt;
import kite.Ast.FieldDecl;
import kite.Ast.ForStmt;
import kite.Ast.Get;
import kite.Ast.IfStmt;
import kite.Ast.Index;
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
    private final Map<String, Map<String, String>> fieldTypesByType = new HashMap<>();
    private final Map<String, Set<String>> methodsByType = new HashMap<>();
    private final Set<String> typeNames = new HashSet<>();
    private final Set<String> arrayTypes = new LinkedHashSet<>();
    private Set<String> currentFields = Set.of();
    private Set<String> currentMethods = Set.of();
    private String currentType = "";
    private boolean currentMethodIsEntrypoint;
    private Set<String> locals = Set.of();
    private Set<String> stackLocals = Set.of();
    private Map<String, String> localTypes = Map.of();
    private int indentLevel;

    String generate(Program program) {
        out.append("#include <stdint.h>\n");
        out.append("#include <stdbool.h>\n");
        out.append("#include <stdio.h>\n");
        out.append("#include <stdlib.h>\n\n");
        out.append("static void console_write(const char* text) { printf(\"%s\", text); }\n\n");
        out.append("static void* bootstrap_alloc(int32_t size) { return malloc(size); }\n\n");

        indexFields(program);
        validateListUsage(program);
        if (usesBootstrapList(program)) {
            emitPointerListRuntime();
        }
        collectArrayTypes(program);

        for (String arrayType : arrayTypes) {
            emitArrayStruct(arrayType);
        }

        for (TypeDecl type : program.types()) {
            emitStruct(type);
        }

        List<String> prototypes = methodPrototypes(program);
        if (!prototypes.isEmpty()) {
            prototypes.forEach(prototype -> out.append(prototype).append(";\n"));
            out.append("\n");
        }

        emitHeapHook(program);

        for (TypeDecl type : program.types()) {
            currentType = type.name();
            currentFields = fieldsByType.getOrDefault(type.name(), Set.of());
            currentMethods = methodsByType.getOrDefault(type.name(), Set.of());
            for (Member member : type.members()) {
                if (member instanceof MethodDecl method) {
                    emitMethod(type, method);
                }
            }
            currentType = "";
            currentFields = Set.of();
            currentMethods = Set.of();
        }

        return out.toString();
    }

    private void emitHeapHook(Program program) {
        if (hasMainOnHeap(program)) {
            out.append("static kite_main kite_owner;\n");
            out.append("static void* kite_on_heap(size_t size) { return main_on_heap(&kite_owner, (int32_t)size); }\n\n");
        } else {
            out.append("static void* kite_on_heap(size_t size) { return bootstrap_alloc((int32_t)size); }\n\n");
        }
    }

    private boolean hasMainOnHeap(Program program) {
        for (TypeDecl type : program.types()) {
            if (!type.name().equals("main")) {
                continue;
            }
            for (Member member : type.members()) {
                if (member instanceof MethodDecl method && method.name().equals("on_heap")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void validateListUsage(Program program) {
        for (TypeDecl type : program.types()) {
            for (Member member : type.members()) {
                if (member instanceof FieldDecl field) {
                    if (field.type().equals("list") && !isBootstrapListField(type.name(), field.type())) {
                        throw new KiteException("list is currently supported only as a type main owner field");
                    }
                } else if (member instanceof MethodDecl method) {
                    if (method.returnType().equals("list")) {
                        throw new KiteException("list is currently supported only as a type main owner field");
                    }
                    if (method.params().stream().anyMatch(param -> param.type().equals("list"))) {
                        throw new KiteException("list is currently supported only as a type main owner field");
                    }
                    method.body().forEach(this::validateListUsage);
                }
            }
        }
    }

    private void validateListUsage(Stmt stmt) {
        if (stmt == null) {
            return;
        }
        if (stmt instanceof VarDecl varDecl && varDecl.type().equals("list")) {
            throw new KiteException("list is currently supported only as a type main owner field");
        }
        if (stmt instanceof IfStmt ifStmt) {
            ifStmt.thenBranch().forEach(this::validateListUsage);
            ifStmt.elseBranch().forEach(this::validateListUsage);
        } else if (stmt instanceof WhileStmt whileStmt) {
            whileStmt.body().forEach(this::validateListUsage);
        } else if (stmt instanceof ForStmt forStmt) {
            validateListUsage(forStmt.initializer());
            forStmt.body().forEach(this::validateListUsage);
        }
    }

    private boolean usesBootstrapList(Program program) {
        for (TypeDecl type : program.types()) {
            for (Member member : type.members()) {
                if (member instanceof FieldDecl field && isBootstrapListField(type.name(), field.type())) {
                    return true;
                }
                if (member instanceof MethodDecl method) {
                    if (method.body().stream().anyMatch(this::usesBootstrapList)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean usesBootstrapList(Stmt stmt) {
        if (stmt == null) {
            return false;
        }
        if (stmt instanceof IfStmt ifStmt) {
            return ifStmt.thenBranch().stream().anyMatch(this::usesBootstrapList)
                    || ifStmt.elseBranch().stream().anyMatch(this::usesBootstrapList);
        }
        if (stmt instanceof WhileStmt whileStmt) {
            return whileStmt.body().stream().anyMatch(this::usesBootstrapList);
        }
        if (stmt instanceof ForStmt forStmt) {
            return usesBootstrapList(forStmt.initializer())
                    || forStmt.body().stream().anyMatch(this::usesBootstrapList);
        }
        return false;
    }

    private void emitPointerListRuntime() {
        out.append("typedef struct kite_pointer_list {\n");
        out.append("    int32_t count;\n");
        out.append("    int32_t capacity;\n");
        out.append("    void** data;\n");
        out.append("} kite_pointer_list;\n\n");
        out.append("static void pointer_list_add(kite_pointer_list* self, void* item) {\n");
        out.append("    if (self->count == self->capacity) {\n");
        out.append("        int32_t next_capacity = self->capacity == 0 ? 8 : self->capacity * 2;\n");
        out.append("        self->data = realloc(self->data, sizeof(void*) * next_capacity);\n");
        out.append("        self->capacity = next_capacity;\n");
        out.append("    }\n");
        out.append("    self->data[self->count] = item;\n");
        out.append("    self->count = self->count + 1;\n");
        out.append("}\n\n");
        out.append("static void pointer_list_delete_all(kite_pointer_list* self) {\n");
        out.append("    for (int32_t i = 0; i < self->count; i = i + 1) {\n");
        out.append("        free(self->data[i]);\n");
        out.append("    }\n");
        out.append("    free(self->data);\n");
        out.append("    self->data = NULL;\n");
        out.append("    self->count = 0;\n");
        out.append("    self->capacity = 0;\n");
        out.append("}\n\n");
    }

    private List<String> methodPrototypes(Program program) {
        List<String> prototypes = new ArrayList<>();
        for (TypeDecl type : program.types()) {
            for (Member member : type.members()) {
                if (member instanceof MethodDecl method && !isEntrypoint(type, method)) {
                    prototypes.add(methodSignature(type, method));
                }
            }
        }
        return prototypes;
    }

    private void indexFields(Program program) {
        for (TypeDecl type : program.types()) {
            typeNames.add(type.name());
            Set<String> fields = new HashSet<>();
            Map<String, String> fieldTypes = new HashMap<>();
            Set<String> methods = new HashSet<>();
            for (Member member : type.members()) {
                if (member instanceof FieldDecl field) {
                    fields.add(field.name());
                    fieldTypes.put(field.name(), field.type());
                } else if (member instanceof MethodDecl method) {
                    methods.add(method.name());
                }
            }
            fieldsByType.put(type.name(), fields);
            fieldTypesByType.put(type.name(), fieldTypes);
            methodsByType.put(type.name(), methods);
        }
    }

    private void collectArrayTypes(Program program) {
        for (TypeDecl type : program.types()) {
            for (Member member : type.members()) {
                if (member instanceof FieldDecl field) {
                    collectArrayType(field.type());
                } else if (member instanceof MethodDecl method) {
                    collectArrayType(method.returnType());
                    method.params().forEach(param -> collectArrayType(param.type()));
                    method.body().forEach(this::collectArrayTypes);
                }
            }
        }
    }

    private void collectArrayTypes(Stmt stmt) {
        if (stmt == null) {
            return;
        }
        if (stmt instanceof VarDecl varDecl) {
            collectArrayType(varDecl.type());
            collectArrayTypes(varDecl.initializer());
        } else if (stmt instanceof DeleteStmt deleteStmt) {
            collectArrayTypes(deleteStmt.expr());
        } else if (stmt instanceof ExprStmt exprStmt) {
            collectArrayTypes(exprStmt.expr());
        } else if (stmt instanceof ReturnStmt returnStmt) {
            collectArrayTypes(returnStmt.value());
        } else if (stmt instanceof IfStmt ifStmt) {
            collectArrayTypes(ifStmt.condition());
            ifStmt.thenBranch().forEach(this::collectArrayTypes);
            ifStmt.elseBranch().forEach(this::collectArrayTypes);
        } else if (stmt instanceof WhileStmt whileStmt) {
            collectArrayTypes(whileStmt.condition());
            whileStmt.body().forEach(this::collectArrayTypes);
        } else if (stmt instanceof ForStmt forStmt) {
            collectArrayTypes(forStmt.initializer());
            collectArrayTypes(forStmt.condition());
            collectArrayTypes(forStmt.increment());
            forStmt.body().forEach(this::collectArrayTypes);
        }
    }

    private void collectArrayTypes(Expr expr) {
        if (expr == null) {
            return;
        }
        if (expr instanceof ArrayNew arrayNew) {
            collectArrayType(arrayNew.elementType() + "[]");
            collectArrayTypes(arrayNew.size());
        } else if (expr instanceof Assign assign) {
            collectArrayTypes(assign.target());
            collectArrayTypes(assign.value());
        } else if (expr instanceof Binary binary) {
            collectArrayTypes(binary.left());
            collectArrayTypes(binary.right());
        } else if (expr instanceof Call call) {
            collectArrayTypes(call.callee());
            call.args().forEach(this::collectArrayTypes);
        } else if (expr instanceof Get get) {
            collectArrayTypes(get.object());
        } else if (expr instanceof Index index) {
            collectArrayTypes(index.object());
            collectArrayTypes(index.index());
        }
    }

    private void collectArrayType(String type) {
        if (isArrayType(type)) {
            arrayTypes.add(type);
        }
    }

    private void emitArrayStruct(String arrayType) {
        String elementType = arrayElementType(arrayType);
        out.append("typedef struct ").append(cArrayName(arrayType)).append(" {\n");
        out.append("    int32_t length;\n");
        out.append("    ").append(cType(elementType)).append("* data;\n");
        out.append("} ").append(cArrayName(arrayType)).append(";\n\n");
    }

    private void emitStruct(TypeDecl type) {
        out.append("typedef struct ").append(cStructName(type.name())).append(" {\n");
        for (Member member : type.members()) {
            if (member instanceof FieldDecl field) {
                out.append("    ").append(cFieldType(type, field)).append(" ").append(field.name()).append(";\n");
            }
        }
        out.append("} ").append(cStructName(type.name())).append(";\n\n");
    }

    private String cFieldType(TypeDecl owner, FieldDecl field) {
        if (isBootstrapListField(owner.name(), field.type())) {
            return "kite_pointer_list";
        }
        return cType(field.type());
    }

    private void emitMethod(TypeDecl type, MethodDecl method) {
        boolean isEntrypoint = isEntrypoint(type, method);
        currentMethodIsEntrypoint = isEntrypoint;
        locals = new HashSet<>();
        stackLocals = new HashSet<>();
        localTypes = new HashMap<>();
        method.params().forEach(param -> {
            locals.add(param.name());
            localTypes.put(param.name(), param.type());
        });

        out.append(isEntrypoint ? "int main(void)" : methodSignature(type, method));

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
        stackLocals = Set.of();
        localTypes = Map.of();
        currentMethodIsEntrypoint = false;
    }

    private boolean isEntrypoint(TypeDecl type, MethodDecl method) {
        return type.name().equals("main") && method.name().equals("main") && method.params().isEmpty();
    }

    private String methodSignature(TypeDecl type, MethodDecl method) {
        List<String> params = new ArrayList<>();
        params.add(cStructName(type.name()) + "* self");
        params.addAll(method.params().stream()
                .map(param -> cType(param.type()) + " " + param.name())
                .toList());
        return cType(method.returnType()) + " " + type.name() + "_" + method.name() + "("
                + String.join(", ", params) + ")";
    }

    private void emitStmt(Stmt stmt) {
        if (stmt instanceof VarDecl varDecl) {
            emitVarDecl(varDecl);
        } else if (stmt instanceof DeleteStmt deleteStmt) {
            validateDelete(deleteStmt);
            line("free(" + expr(deleteStmt.expr()) + ");");
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

    private void emitVarDecl(VarDecl varDecl) {
        locals.add(varDecl.name());
        localTypes.put(varDecl.name(), varDecl.type());
        if (varDecl.stack()) {
            stackLocals.add(varDecl.name());
        }

        if (isArrayType(varDecl.type())) {
            emitArrayVarDecl(varDecl);
        } else if (isKiteObject(varDecl.type())) {
            if (varDecl.stack()) {
                line(cStructName(varDecl.type()) + " " + storageName(varDecl.name()) + ";");
                line(cType(varDecl.type()) + " " + varDecl.name() + " = &" + storageName(varDecl.name()) + ";");
                emitObjectInitializer(varDecl);
            } else if (varDecl.initializer() != null) {
                if (isInitializerCall(varDecl)) {
                    line(cType(varDecl.type()) + " " + varDecl.name() + " = kite_on_heap(sizeof(" + cStructName(varDecl.type()) + "));");
                    emitObjectInitializer(varDecl);
                } else {
                    line(cType(varDecl.type()) + " " + varDecl.name() + " = " + expr(varDecl.initializer()) + ";");
                }
            } else {
                line(cType(varDecl.type()) + " " + varDecl.name() + " = kite_on_heap(sizeof(" + cStructName(varDecl.type()) + "));");
            }
        } else if (isInitializerCall(varDecl)) {
            line(cType(varDecl.type()) + " " + varDecl.name() + ";");
            Call initCall = (Call) varDecl.initializer();
            String args = initCall.args().stream().map(this::expr).collect(Collectors.joining(", "));
            String allArgs = args.isEmpty() ? "&" + varDecl.name() : "&" + varDecl.name() + ", " + args;
            line(varDecl.type() + "_init(" + allArgs + ");");
        } else {
            indent();
            out.append(cType(varDecl.type())).append(" ").append(varDecl.name());
            if (varDecl.initializer() != null) {
                out.append(" = ").append(expr(varDecl.initializer()));
            }
            out.append(";\n");
        }
    }

    private void validateDelete(DeleteStmt deleteStmt) {
        if (deleteStmt.expr() instanceof Variable variable && stackLocals.contains(variable.name())) {
            throw new KiteException("Cannot delete stack object '" + variable.name() + "'");
        }
    }

    private void emitObjectInitializer(VarDecl varDecl) {
        if (isInitializerCall(varDecl)) {
            Call initCall = (Call) varDecl.initializer();
            String args = initCall.args().stream().map(this::expr).collect(Collectors.joining(", "));
            String allArgs = args.isEmpty() ? varDecl.name() : varDecl.name() + ", " + args;
            line(varDecl.type() + "_init(" + allArgs + ");");
        } else if (varDecl.initializer() != null) {
            line(varDecl.name() + " = " + expr(varDecl.initializer()) + ";");
        }
    }

    private void emitArrayVarDecl(VarDecl varDecl) {
        line(cArrayName(varDecl.type()) + " " + storageName(varDecl.name()) + ";");
        line(cType(varDecl.type()) + " " + varDecl.name() + " = &" + storageName(varDecl.name()) + ";");
        if (varDecl.initializer() instanceof ArrayNew arrayNew) {
            String size = expr(arrayNew.size());
            String elementType = arrayElementType(varDecl.type());
            line(varDecl.name() + "->length = " + size + ";");
            line(varDecl.name() + "->data = calloc(" + size + ", sizeof(" + cType(elementType) + "));");
        } else if (varDecl.initializer() != null) {
            line(varDecl.name() + " = " + expr(varDecl.initializer()) + ";");
        }
    }

    private boolean isInitializerCall(VarDecl varDecl) {
        if (!typeNames.contains(varDecl.type()) || !(varDecl.initializer() instanceof Call call)) {
            return false;
        }
        return call.callee() instanceof Variable variable && variable.name().equals(varDecl.type());
    }

    private String forInitializer(Stmt initializer) {
        if (initializer == null) {
            return "";
        }
        if (initializer instanceof VarDecl varDecl) {
            locals.add(varDecl.name());
            localTypes.put(varDecl.name(), varDecl.type());
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
        if (expr instanceof ArrayNew) {
            throw new IllegalStateException("Array creation is only supported in variable initializers");
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
            if (get.object() instanceof Variable variable && variable.name().equals("allocator") && get.name().equals("alloc")) {
                return "bootstrap_alloc";
            }
            if (get.name().equals("length") && isArrayExpr(get.object())) {
                return expr(get.object()) + "->length";
            }
            if (get.object() instanceof Variable variable && isKiteObject(localTypes.get(variable.name()))) {
                return expr(get.object()) + "->" + get.name();
            }
            return expr(get.object()) + "." + get.name();
        }
        if (expr instanceof Index index) {
            return expr(index.object()) + "->data[" + expr(index.index()) + "]";
        }
        if (expr instanceof Call call) {
            if (call.callee() instanceof Variable variable && currentMethods.contains(variable.name())) {
                String args = call.args().stream().map(this::expr).collect(Collectors.joining(", "));
                String selfArg = currentMethodIsEntrypoint && currentType.equals("main") ? "&kite_owner" : "self";
                String allArgs = args.isEmpty() ? selfArg : selfArg + ", " + args;
                return currentType + "_" + variable.name() + "(" + allArgs + ")";
            }
            if (call.callee() instanceof Get get && get.object() instanceof Variable variable) {
                if (variable.name().equals("allocator")) {
                    validateAllocatorCall(get, call);
                    String args = call.args().stream().map(this::expr).collect(Collectors.joining(", "));
                    return "bootstrap_alloc(" + args + ")";
                }
                String objectType = localTypes.get(variable.name());
                if (objectType != null && typeNames.contains(objectType)) {
                    String args = call.args().stream().map(this::expr).collect(Collectors.joining(", "));
                    String selfArg = variable.name();
                    String allArgs = args.isEmpty() ? selfArg : selfArg + ", " + args;
                    return objectType + "_" + get.name() + "(" + allArgs + ")";
                }
                String fieldType = fieldTypesByType.getOrDefault(currentType, Map.of()).get(variable.name());
                if (fieldType != null && isBootstrapListField(currentType, fieldType)) {
                    validateOwnerListCall(get, call);
                    String args = call.args().stream().map(this::expr).collect(Collectors.joining(", "));
                    String selfArg = "&" + expr(get.object());
                    String allArgs = args.isEmpty() ? selfArg : selfArg + ", " + args;
                    return "pointer_list_" + get.name() + "(" + allArgs + ")";
                }
            }
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

    private void validateAllocatorCall(Get get, Call call) {
        if (!currentType.equals("main")) {
            throw new KiteException("allocator.alloc is currently supported only inside type main");
        }
        if (!get.name().equals("alloc")) {
            throw new KiteException("Unsupported allocator method '" + get.name() + "'");
        }
        if (call.args().size() != 1) {
            throw new KiteException("allocator.alloc expects 1 argument");
        }
    }

    private void validateOwnerListCall(Get get, Call call) {
        if (get.name().equals("add")) {
            if (call.args().size() != 1) {
                throw new KiteException("owner list add expects 1 argument");
            }
            return;
        }
        if (get.name().equals("delete_all")) {
            if (!call.args().isEmpty()) {
                throw new KiteException("owner list delete_all expects 0 arguments");
            }
            return;
        }
        throw new KiteException("Unsupported owner list method '" + get.name() + "'");
    }

    private String cType(String kiteType) {
        if (isArrayType(kiteType)) {
            return cArrayName(kiteType) + "*";
        }
        if (kiteType.equals("list")) {
            return "kite_list*";
        }
        if (kiteType.startsWith("pointer ")) {
            return cPointerType(kiteType.substring("pointer ".length()));
        }
        if (kiteType.equals("pointer")) {
            return "void*";
        }
        if (isKiteObject(kiteType)) {
            return cStructName(kiteType) + "*";
        }
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

    private String cPointerType(String kiteType) {
        if (typeNames.contains(kiteType)) {
            return "struct " + cStructName(kiteType) + "*";
        }
        return cType(kiteType) + "*";
    }

    private String cStructName(String kiteType) {
        return "kite_" + kiteType;
    }

    private boolean isKiteObject(String kiteType) {
        return typeNames.contains(kiteType);
    }

    private boolean isBootstrapListField(String ownerType, String fieldType) {
        return ownerType.equals("main") && fieldType.equals("list");
    }

    private String storageName(String variableName) {
        return "_" + variableName + "_storage";
    }

    private boolean isArrayType(String kiteType) {
        return kiteType != null && kiteType.endsWith("[]");
    }

    private String arrayElementType(String arrayType) {
        return arrayType.substring(0, arrayType.length() - 2);
    }

    private String cArrayName(String arrayType) {
        return "kite_array_" + mangleType(arrayElementType(arrayType));
    }

    private String mangleType(String kiteType) {
        return kiteType.replace("pointer ", "pointer_").replace("[]", "_array");
    }

    private boolean isArrayExpr(Expr expr) {
        return expr instanceof Variable variable && isArrayType(localTypes.get(variable.name()));
    }
}
