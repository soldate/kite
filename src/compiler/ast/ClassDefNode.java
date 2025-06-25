package compiler.ast;

import java.util.List;

public class ClassDefNode extends Node {
    public String name;
    public List<VarDeclNode> fields;
    public List<FuncDefNode> methods;
    
    public ClassDefNode(String name, List<VarDeclNode> fields, List<FuncDefNode> methods) {
        this.name = name;
        this.fields = fields;
        this.methods = methods;

        for (FuncDefNode method : methods) {
            method.parentClass = this;
        }

        for (VarDeclNode field : fields) {
            field.parentClass = this;
        }
    }

	@Override
	public String toString() {
		return "ClassDefNode (name=" + name + ")";
	}      
}
