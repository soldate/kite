package compiler.ast.core;

import java.util.LinkedHashMap;
import java.util.Map;

import compiler.ast.stmt.FuncDefNode;

public class ProgramNode extends Node {

    public final Map<String, ClassNode> types = new LinkedHashMap<>();
    public FuncDefNode main;

    @Override
    public String toString() {
        return "ProgramNode (types=" + types.keySet() + ")";
    }
}
