package compiler.ast;

import compiler.Parser;

public class WhileNode extends Node {
	public Node cond;
	public Node body;

	public WhileNode(Node cond, Node body) {
		this.cond = cond;
		this.body = body;
		this.currentBlock = Parser.currentBlock;
	}

	@Override
	public String toString() {
		return "WhileNode (cond=" + cond + ")";
	}
}
