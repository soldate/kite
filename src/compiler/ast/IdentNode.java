package compiler.ast;

import compiler.Parser;

public class IdentNode extends Node {
	public String name;

	public IdentNode(String name) {
		this.name = name;
	}	

	@Override
	public String toString() {
		return "IdentNode (name=" + name + ")";
	}

	public VarDeclNode getVarDeclNode() {
		if (currentBlock != null) {
			VarDeclNode varDecl = currentBlock.localsMap.get(name);
			if (varDecl != null) {
				return varDecl;
			}
		}
		return null;
	}
}