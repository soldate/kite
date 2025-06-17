package compiler.ast;

import java.util.ArrayList;
import java.util.List;

public class BlockNode extends Node {
	public List<Node> statements = new ArrayList<>();

	@Override
	public String toString() {
		return "BlockNode (stmts.len=" + statements.size() + ")";
	}

}
