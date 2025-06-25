package compiler.ast;

import compiler.Parser;

public class IdentNode extends Node {
	public String name;

	public IdentNode(String name) {
		this.name = name;
		this.currentBlock = Parser.currentBlock;
	}	

	@Override
	public String toString() {
		return "IdentNode (name=" + name + ")";
	}
}