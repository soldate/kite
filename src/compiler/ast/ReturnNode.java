package compiler.ast;

public class ReturnNode extends Node {
	public final Node expr; // pode ser null

	public ReturnNode(Node expr) {
		this.expr = expr;
	}
}
