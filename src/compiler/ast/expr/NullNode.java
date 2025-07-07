package compiler.ast.expr;

import compiler.ast.core.*;

// null keyword, used to avoid cyclic dependencies. 
// check checkForCycles() in Parser.parse() 
public class NullNode extends Node {
	@Override
	public String toString() {
		return "NullNode";
	}
}
