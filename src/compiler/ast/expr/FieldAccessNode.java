package compiler.ast.expr;

import compiler.ast.core.Node;

public class FieldAccessNode extends Node {
    public Node target;
    public String field;

    public FieldAccessNode(Node target, String field) {
        this.target = target;
        this.field = field;
    }

	@Override
	public String toString() {
		return "FieldAccessNode (field=" + field + ", target=" + target + ")";
	}    
}
