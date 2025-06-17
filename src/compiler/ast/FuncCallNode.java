package compiler.ast;

import java.util.List;

public class FuncCallNode extends Node {
	public String name;
	public List<Node> args;

	public FuncCallNode(String name, List<Node> args) {
		this.name = name;
		this.args = args;
	}

	@Override
	public String toString() {
		return "FuncCallNode (name=" + name + ")";
	}
}
