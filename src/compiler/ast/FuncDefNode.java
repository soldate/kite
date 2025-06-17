package compiler.ast;

import java.util.List;

public class FuncDefNode extends Node {
	public final String name;
	public final List<String> params;
	public final BlockNode body;
	public final String returnType; // novo campo

	public FuncDefNode(String name, List<String> params, BlockNode body, String returnType) {
		this.name = name;
		this.params = params;
		this.body = body;
		this.returnType = returnType;
	}

	@Override
	public String toString() {
		return "FuncDefNode (name=" + name + ")";
	}
}
