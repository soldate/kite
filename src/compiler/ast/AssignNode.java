package compiler.ast;

public class AssignNode extends Node {
	public final IdentNode target;
	public final Node value;

	public AssignNode(IdentNode target, Node value) {
		this.target = target;
		this.value = value;
	}

	public AssignNode(String name, Node value) {
		this.target = new IdentNode(name);
		this.value = value;
	}
}
