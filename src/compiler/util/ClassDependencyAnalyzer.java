package compiler.util;

import java.util.*;
import compiler.ast.expr.NullNode;
import compiler.ast.var_def.VarDeclNode;

public class ClassDependencyAnalyzer {

    private final Map<String, Set<String>> graph = new HashMap<>();
    private final Set<String> visited = new HashSet<>();
    private final Set<String> recursionStack = new HashSet<>();

    public void addFieldDependency(String fromClass, VarDeclNode field) {
        if (field.typeClass == null) return;
        if (field.value instanceof NullNode) return; // Trata como ponteiro, não inline

        graph.computeIfAbsent(fromClass, k -> new HashSet<>()).add(field.typeClass.name);
    }

    public void checkForCycles() {
        for (String className : graph.keySet()) {
            visited.clear();
            recursionStack.clear();
            if (isCyclic(className)) {
                throw new RuntimeException("Referência cíclica detectada envolvendo a classe: " + className + ". Use '= null' para quebrar o ciclo.");
            }
        }
    }

    private boolean isCyclic(String current) {
        if (recursionStack.contains(current)) return true;
        if (visited.contains(current)) return false;

        visited.add(current);
        recursionStack.add(current);

        for (String neighbor : graph.getOrDefault(current, Collections.emptySet())) {
            if (isCyclic(neighbor)) return true;
        }

        recursionStack.remove(current);
        return false;
    }
} 
