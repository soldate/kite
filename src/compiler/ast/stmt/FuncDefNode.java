package compiler.ast.stmt;

import compiler.ast.core.ClassDefNode;
import compiler.ast.core.Node;
import compiler.ast.var_def.ParamFuncDefNode;
import compiler.ast.var_def.VarDeclNode;
import java.util.LinkedHashMap;
import java.util.Map;

// ex: int add(arg1, arg2, ...)
public class FuncDefNode extends Node {
	public String name;
	public Map<String, VarDeclNode> params = new LinkedHashMap<>();
	public String returnType;
	public ClassDefNode clazz;
	public BlockNode body;

	public FuncDefNode(String returnType, String name) {		
		this.returnType = returnType;
		this.name = name;			
	}

	public FuncDefNode(ClassDefNode clazz, String returnType, String name) {		
		this.returnType = returnType;
		this.clazz = clazz;
		this.name = clazz.name + "_" + name;			
		this.clazz.methods.put(name, this);
		new ParamFuncDefNode(this, clazz.name, "this");
	}

	@Override
	public String toString() {
		return "FuncDefNode (name=" + name + ")";
	}
}
