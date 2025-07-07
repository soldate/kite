package compiler.ast.var_def;

import java.util.*;

import compiler.ast.core.*;

// class field, function paramater or local variable declaration
public abstract class VarDeclNode extends Node {

	// during parser, we have only type names (string) in VarDeclNode objects.
	// When parser finishes, we will have all types (classes) defined,
	// so we can resolve typeClass variable (see below).
	// this list is used to resolve typeClass after parsing is done.
	public static List<VarDeclNode> allVars = new ArrayList<>();

	public String name;
	public String type;
	public Node value;

	public ClassDefNode typeClass;

	protected VarDeclNode(String type, String name, Node value) {
		this.name = name;
		this.value = value;
		this.type = type;

		allVars.add(this);
	}

}
