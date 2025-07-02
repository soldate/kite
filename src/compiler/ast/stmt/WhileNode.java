package compiler.ast.stmt;

import compiler.ast.core.Node;

public class WhileNode extends Node {
	public Node cond;
	public Node body;

	@Override
	public String toString() {
		return "WhileNode (cond=" + cond + ")";
	}
}
