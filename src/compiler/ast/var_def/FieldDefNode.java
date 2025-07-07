package compiler.ast.var_def;

import compiler.ast.core.*;

// Represents a field definition in a class.
public class FieldDefNode extends VarDeclNode {

	public boolean isStackAllocated;
	public ClassDefNode clazz;

	public FieldDefNode(ClassDefNode clazz, String type, String name) {
		super(type, name, null);
		this.clazz = clazz;
		this.name = name;
		this.clazz.fields.put(name, this);
	}

	public FieldDefNode(ClassDefNode clazz, String type, String name, Node value) {
		this(clazz, type, name);
		this.value = value;
	}

	@Override
	public String toString() {
		return "FieldDefNode (name=" + name + ", type=" + type + ", value=" + value + ")";
	}
}
