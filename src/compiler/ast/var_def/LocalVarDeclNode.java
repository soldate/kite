package compiler.ast.var_def;

import compiler.ast.core.*;
import compiler.ast.stmt.*;

public class LocalVarDeclNode extends VarDeclNode {

	public BlockNode block;

	public LocalVarDeclNode(BlockNode block, String type, String name) {
		super(type, name, null);
		this.block = block;
		block.varLocals.put(name, this);
	}

	public LocalVarDeclNode(BlockNode block, String type, String name, Node value) {
		this(block, type, name);
		this.value = value;
	}

	@Override
	public String toString() {
		return "FieldDefNode (name=" + name + ", type=" + type + ", value=" + value + ")";
	}
}
