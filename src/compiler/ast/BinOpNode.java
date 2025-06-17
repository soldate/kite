package compiler.ast;

import compiler.Token;

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
