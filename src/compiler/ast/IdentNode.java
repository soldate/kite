package compiler.ast;

public class IdentNode extends Node {
	public String name;

	public IdentNode(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "IdentNode (name=" + name + ")";
	}
}