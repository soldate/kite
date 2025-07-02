package compiler.ast.expr;

import compiler.Token;
import compiler.ast.core.Node;

// ex: a + b, a < b, a == b, etc
public class BinOpNode extends Node {
	public Node left;
	public Node right;
	public Token.Kind op;

	public BinOpNode(Node left, Token.Kind op, Node right) {
		this.left = left;
		this.op = op;
		this.right = right;
	}

	@Override
	public String toString() {
		return "BinOpNode (left: " + left.toString() + " right: " + right.toString() + " op: " + op + ")";
	}

}
