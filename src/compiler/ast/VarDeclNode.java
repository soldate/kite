package compiler.ast;

public class VarDeclNode extends Node {
	public String name;
	public String type;
	public Node value;

	public VarDeclNode(String type, String name, Node value) {
		this.type = type;
		this.name = name;
		this.value = value;
	}

	@Override
	public String toString() {
		return "VarDeclNode (name=" + name + ", type=" + type + ", value=" + value + ")";
	}
}
