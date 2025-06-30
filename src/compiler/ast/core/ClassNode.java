package compiler.ast.core;

import java.util.LinkedHashMap;
import java.util.Map;

import compiler.ast.stmt.FuncDefNode;
import compiler.ast.var_def.VarDeclNode;

public class ClassNode extends Node {
    public String name;
    public ProgramNode prog;
    public Map<String, VarDeclNode> fields = new LinkedHashMap<>();
	public Map<String, FuncDefNode> methods = new LinkedHashMap<>();
    
    public ClassNode(ProgramNode prog, String name) {
        this.name = name;
        this.prog = prog;
        this.prog.types.put(name, this);		
    }

	@Override
	public String toString() {
		return "ClassDefNode (name=" + name + ")";
	}      
}
