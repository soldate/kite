package compiler.ast.core;

import java.util.*;

import compiler.ast.stmt.*;

public class ProgramNode extends Node {

	public final Map<String, String> imports = new LinkedHashMap<>();
	public final Map<String, ClassDefNode> types = new LinkedHashMap<>();
	public FuncDefNode main;
	public String packageName = null;

	@Override
	public String toString() {
		return "ProgramNode (types=" + types.keySet() + ")";
	}
}
