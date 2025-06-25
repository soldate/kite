package compiler.ast;

import java.util.List;

import compiler.Parser;

public class FuncDefNode extends Node {
	public String name;
	public List<VarDeclNode> params;
	public BlockNode body;
	public String returnType;

	public ClassDefNode parentClass;

	public FuncDefNode(String name, List<VarDeclNode> params, BlockNode body, String returnType) {
		this.name = name;
		if (!"main".equals(name)) this.name = Parser.currentClassName + "_" + name;
		this.params = params;
		this.body = body;
		this.returnType = returnType;

		body.parentFunc = this;
	}

	@Override
	public String toString() {
		return "FuncDefNode (name=" + name + ")";
	}
}
