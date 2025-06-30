package compiler.ast.stmt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import compiler.ast.core.Node;
import compiler.ast.var_def.VarDeclNode;

public class BlockNode extends Node {
	public List<Node> statements = new ArrayList<>();
	public Map<String, VarDeclNode> varLocals = new LinkedHashMap<>();
	
	public FuncDefNode fn;
	public WhileNode whileNode;
	public IfNode ifNode;
	
	public BlockNode parentBlock;	

	public BlockNode(BlockNode parentBlock, FuncDefNode fn) {
		this.fn = fn;
		this.fn.block = this;
		this.parentBlock = parentBlock;
	}

	public BlockNode(BlockNode parentBlock, WhileNode whileNode) {
        this.whileNode = whileNode;
		this.whileNode.block = this;
		this.parentBlock = parentBlock;
    }

	public BlockNode(BlockNode parentBlock, IfNode ifNode) {
        this.ifNode = ifNode;
		this.parentBlock = parentBlock;
    }	

    @Override
	public String toString() {
		return "BlockNode (stmts.len=" + statements.size() + ")";
	}

	public VarDeclNode findVarDecl(String var) {
		// local var
		VarDeclNode decl = varLocals.get(var);
		if (decl != null) {
			return decl;
		}
		// function parameter
		if (fn != null) {
			decl = fn.params.get(var);
			if (decl != null) {
				return decl;
			}		
		}
		// parent block local
		BlockNode b = parentBlock;
		while(b != null) {
			decl = b.varLocals.get(var);
			if (decl != null) {
				return decl;
			}
			b = b.parentBlock;	
		}
		throw new RuntimeException(var + " not exists");
	}

}
