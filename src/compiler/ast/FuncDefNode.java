package compiler.ast;

import java.util.List;

public class FuncDefNode extends Node {
	public String name;
	public List<String> params;
	public BlockNode body;

	public FuncDefNode(String name, List<String> params, BlockNode body) {
		this.name = name;
		this.params = params;
		this.body = body;
	}
}