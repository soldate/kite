package compiler.ast.expr;

import compiler.ast.core.Node;
import compiler.ast.stmt.BlockNode;
import compiler.ast.var_def.VarDeclNode;

public class IdentNode extends Node {
	public VarDeclNode varDecl;
	public BlockNode block;
	
	public IdentNode(BlockNode block, VarDeclNode varDecl) {
		this.block = block;
		this.varDecl = varDecl;
	}	

	@Override
	public String toString() {
		return "IdentNode (name=" + varDecl.name + ")";
	}

}