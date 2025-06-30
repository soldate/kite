package compiler.ast.stmt;

import compiler.Parser;
import compiler.ast.core.Node;

public class ReturnNode extends Node {
	public final Node expr; // pode ser null

	public ReturnNode(Node expr) {
		this.expr = expr;
	}

	@Override
	public String toString() {
		return "ReturnNode (expr=" + expr + ")";
	}
}
