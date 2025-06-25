package compiler.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockNode extends Node {
	private List<Node> statements = new ArrayList<>();
	public List<VarDeclNode> locals = new ArrayList<>();
	public Map<String, VarDeclNode> localsMap = new HashMap<>();

	public FuncDefNode parentFunc;

    public BlockNode() {
        this.currentBlock = this;
    }	

	public void addStatement(Node statement) {		
		if (statement != null) {
			statement.currentBlock = this;
			statements.add(statement);			
		}
	}

	public List<Node> getStatements() {
		return statements;
	}

	@Override
	public String toString() {
		return "BlockNode (stmts.len=" + statements.size() + ")";
	}

}
