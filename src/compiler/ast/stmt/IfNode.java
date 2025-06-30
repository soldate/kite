package compiler.ast.stmt;

import compiler.ast.core.Node;

public class IfNode extends Node {
	public Node cond;
	public Node thenBranch;
	public Node elseBranch;
	public Node block;

	@Override
	public String toString() {
		return "IfNode (cond=" + cond + ")";
	}
}