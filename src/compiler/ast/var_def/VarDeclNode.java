package compiler.ast.var_def;

import java.util.ArrayList;
import java.util.List;

import compiler.ast.core.ClassNode;
import compiler.ast.core.Node;

// class field, function paramater or local variable declaration
public abstract class VarDeclNode extends Node {
	
	public static List<VarDeclNode> allVars = new ArrayList<>();

	public String name;
	public String type;
	public Node value;

	public ClassNode typeClass;

	protected VarDeclNode(String type, String name, Node value) {
		this.name = name;
		this.value = value;
		this.type = type;

		allVars.add(this);
	}	

}
