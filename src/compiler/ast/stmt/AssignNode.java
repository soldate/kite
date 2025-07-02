package compiler.ast.stmt;

import compiler.ast.core.Node;


public class AssignNode extends Node {
    public final Node target;
    public final Node value;

    public AssignNode(Node target, Node value) {
        this.target = target;
        this.value = value;
    }

	@Override
	public String toString() {
		return "AssignNode (target: " + target + " value: " + value + ")";
	}    
}
