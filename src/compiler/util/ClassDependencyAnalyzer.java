package compiler.util;

import compiler.ast.expr.NullNode;
import compiler.ast.var_def.VarDeclNode;
import java.util.*;

/*
// The code below is problematic, it has a cyclic dependency.
// A has a field of type B, and B has a field of type A.
// It's a problema because 'A a;'' is not just a pointer 'a', 
// but a stack allocation of object A too.
// To avoid this, we should initialize some the field to null
// to broke the cycle.
//
// A a = null; In this case 'a' is just a pointer, no allocation.

class A {
    B b;
}

class B {
    A a; // should be 'A a = null;' to compile
}
*/
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
