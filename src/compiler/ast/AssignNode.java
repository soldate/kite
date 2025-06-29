package compiler.ast;

import compiler.Parser;

public class AssignNode extends Node {
    public final Node target;
    public final Node value;

    public AssignNode(Node target, Node value) {
        this.target = target;
        this.value = value;
    }
}
