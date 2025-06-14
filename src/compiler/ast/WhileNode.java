package compiler.ast;

public class WhileNode extends Node {
	public Node cond;
	public Node body;

	public WhileNode(Node cond, Node body) {
		this.cond = cond;
		this.body = body;
	}
}
