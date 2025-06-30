package compiler.ast.stmt;

import java.util.LinkedHashMap;
import java.util.Map;

import compiler.ast.core.ClassNode;
import compiler.ast.core.Node;
import compiler.ast.var_def.ParamFuncDefNode;
import compiler.ast.var_def.VarDeclNode;

public class FuncDefNode extends Node {
	public String name;
	public Map<String, VarDeclNode> params = new LinkedHashMap<>();
	public String returnType;
	public ClassNode clazz;
	public BlockNode block;

	public FuncDefNode(String returnType, String name) {		
		this.returnType = returnType;
		this.name = name;			
	}

	public FuncDefNode(ClassNode clazz, String returnType, String name) {		
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
