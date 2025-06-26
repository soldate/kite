package compiler.ast;

import java.util.List;

import compiler.Parser;

public class FuncCallNode extends Node {
	public String name;
	public List<Node> args;

	public FuncCallNode(String name, List<Node> args) {
		this.name = name;
		if (!"main".equals(name)) this.name = Parser.currentClassName + "_" + name;
		this.args = args;
	}

	@Override
	public String toString() {
		return "FuncCallNode (name=" + name + ")";
	}
}
