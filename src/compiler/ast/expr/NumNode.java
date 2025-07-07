package compiler.ast.expr;

import compiler.ast.core.*;

public class NumNode extends Node {
	public int value;

	public NumNode(int value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "NumNode (value=" + value + ")";
	}
}
