package compiler.ast;

import java.util.List;

import compiler.Parser;

public class FuncCallNode extends Node {
	public String var;
	public String name;
	public List<Node> args;

	public FuncCallNode(String var, String name, List<Node> args) {
		this.var = var;
		if (!"main".equals(name)) this.name = currentBlock.localsMap.get(var).type + "_" + name;
		this.args = args;
	}

	@Override
	public String toString() {
		return "FuncCallNode (name=" + name + ")";
	}
}
