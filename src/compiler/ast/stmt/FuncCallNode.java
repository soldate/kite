package compiler.ast.stmt;

import compiler.ast.core.Node;
import compiler.ast.expr.IdentNode;
import compiler.ast.var_def.VarDeclNode;
import java.util.ArrayList;
import java.util.List;

// ex: func_name(arg1, arg2, ...)
public class FuncCallNode extends Node {
	public VarDeclNode target;
	public String name;
	public List<Node> args = new ArrayList<>();
	public BlockNode body;
	
	public FuncCallNode(BlockNode body, VarDeclNode target, String name) {
		this.body = body;
		this.target = target;
		this.name = target.type + "_" +  name;		
		this.args.add(new IdentNode(body, target));
	}

	@Override
	public String toString() {
		return "FuncCallNode (name=" + name + ")";
	}
}
