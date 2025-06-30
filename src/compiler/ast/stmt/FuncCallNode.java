package compiler.ast.stmt;

import java.util.ArrayList;
import java.util.List;

import compiler.ast.core.Node;
import compiler.ast.expr.IdentNode;
import compiler.ast.var_def.VarDeclNode;

public class FuncCallNode extends Node {
	public VarDeclNode target;
	public String name;
	public List<Node> args = new ArrayList<>();
	public BlockNode block;
	
	public FuncCallNode(BlockNode block, VarDeclNode target, String name) {
		this.block = block;
		this.target = target;
		this.name = target.type + "_" +  name;		
		this.args.add(new IdentNode(block, target));
	}

	@Override
	public String toString() {
		return "FuncCallNode (name=" + name + ")";
	}
}
