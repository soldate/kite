package compiler.ast;

import compiler.Parser;

public class IfNode extends Node {
	public Node cond;
	public Node thenBranch;
	public Node elseBranch;

	public IfNode(Node cond, Node thenBranch) {
		this.cond = cond;
		this.thenBranch = thenBranch;
	}

	@Override
	public String toString() {
		return "IfNode (cond=" + cond + ")";
	}
}