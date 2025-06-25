package compiler.ast;

import compiler.Parser;

public class VarDeclNode extends Node {
	public String name;
	public String type;
	public Node value;

	public ClassDefNode parentClass;

	public VarDeclNode(String type, String name) {
		this(type, name, null);
	}

	public VarDeclNode(String type, String name, Node value) {
		this.name = name;
		this.value = value;
		this.type = type;

		this.currentBlock = Parser.currentBlock;
		if (currentBlock != null) {
			currentBlock.locals.add(this);
			currentBlock.localsMap.put(name, this);
		}
	}

	@Override
	public String toString() {
		return "VarDeclNode (name=" + name + ", type=" + type + ", value=" + value + ")";
	}
}
