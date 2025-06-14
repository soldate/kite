package compiler.ast;

public class IfNode extends Node {
	public Node cond;
	public Node thenBranch;

	public IfNode(Node cond, Node thenBranch) {
		this.cond = cond;
		this.thenBranch = thenBranch;
	}
}