package compiler.ast;

import compiler.Parser;

public class NumNode extends Node {
	public int value;

	public NumNode(int value) {
		this.value = value;
		this.currentBlock = Parser.currentBlock;
	}

	@Override
	public String toString() {
		return "NumNode (value=" + value + ")";
	}
}
