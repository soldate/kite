package compiler.ast;

public class ReturnNode extends Node {
	public Node expr;

	public ReturnNode(Node expr) {
		this.expr = expr;
	}
}