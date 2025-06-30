package compiler.ast.var_def;

import compiler.ast.stmt.FuncDefNode;

public class ParamFuncDefNode extends VarDeclNode {

	public FuncDefNode fn;

	public ParamFuncDefNode(FuncDefNode fn, String type, String name) {
		super(type, name, null);
		this.fn = fn;
		fn.params.put(name, this);
	}

	@Override
	public String toString() {
		return "ParamFuncDefNode (name=" + name + ", type=" + type + ", value=" + value + ")";
	}    
}
