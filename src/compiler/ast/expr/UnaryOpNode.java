package compiler.ast.expr;

import compiler.ast.core.Node;
import compiler.Token.Kind;

public class UnaryOpNode extends Node {
    public final Kind op;
    public final Node expr;

    public UnaryOpNode(Kind op, Node expr) {
        this.op = op;
        this.expr = expr;
    }
}

