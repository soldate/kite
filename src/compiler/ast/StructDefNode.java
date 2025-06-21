package compiler.ast;

import java.util.List;

public class StructDefNode extends Node {
    public String name;
    public List<VarDeclNode> fields;
    public StructDefNode(String name, List<VarDeclNode> fields) {
        this.name = name;
        this.fields = fields;
    }
}
