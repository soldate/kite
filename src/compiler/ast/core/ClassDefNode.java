package compiler.ast.core;

import compiler.ast.stmt.FuncDefNode;
import compiler.ast.var_def.VarDeclNode;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClassDefNode extends Node {
    
    public ProgramNode prog;

    public String name;    
    public Map<String, VarDeclNode> fields = new LinkedHashMap<>();
	public Map<String, FuncDefNode> methods = new LinkedHashMap<>();
    
    public ClassDefNode(ProgramNode prog, String name) {
        this.name = name;
        this.prog = prog;
        this.prog.types.put(name, this);		
    }

	@Override
	public String toString() {
		return "ClassDefNode (name=" + name + ")";
	}      
}
