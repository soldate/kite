package compiler.ast.core;

import compiler.ast.stmt.FuncDefNode;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProgramNode extends Node {

    public final Map<String, ClassDefNode> types = new LinkedHashMap<>();
    public FuncDefNode main;

    @Override
    public String toString() {
        return "ProgramNode (types=" + types.keySet() + ")";
    }
}
