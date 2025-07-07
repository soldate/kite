
package compiler;

import java.util.*;

import compiler.Token.*;
import compiler.ast.core.*;
import compiler.ast.expr.*;
import compiler.ast.stmt.*;
import compiler.ast.var_def.*;
import compiler.util.*;

public class Parser {
	public static Token current;
	private static BlockNode currentBlock;

	private static Node currentStatement;
	private final Lexer lexer;

	Parser(Lexer lexer) {
		this.lexer = lexer;
		current = lexer.current();
	}

	private Node assign() {
		Node target = compare();
		if (current.kind == Token.Kind.ASSIGN) {
			eat(Token.Kind.ASSIGN);
			Node value = assign();

			if (!(target instanceof IdentNode || target instanceof FieldAccessNode)) {
				throw new RuntimeException("Invalid lvalue in assignment");
			}

			return new AssignNode(target, value);
		}
		return target;
	}

	private ClassDefNode classDef(ProgramNode prog) {
		eat(Token.Kind.CLASS);
		ClassDefNode clazz = new ClassDefNode(prog, current.text);
		eat(Token.Kind.IDENT);
		eat(Token.Kind.LBRACE);

		while (current.kind != Token.Kind.RBRACE) {

			if (isFuncDef()) {
				funcDef(clazz);

			} else if (current.kind == Token.Kind.TYPE) {
				varDecl(clazz, null, null);

			} else {
				throw new RuntimeException("Unexpected token in class: " + current);
			}
		}

		eat(Token.Kind.RBRACE);
		return clazz;
	}

	private void eat(Token.Kind kind) {
		if (current.kind == kind) {
			current = lexer.advance();
		} else {
			throw new RuntimeException(
					"Expected: " + kind + ", but found: " + current.kind + " (" + current.text + ")");
		}
	}

	private FuncDefNode funcDef(ClassDefNode clazz) {
		String returnType = current.text;
		eat(Token.Kind.TYPE);
		String methodName = current.text;
		FuncDefNode fn = null;

		if (clazz == null) fn = new FuncDefNode(returnType, methodName); // main function
		else fn = new FuncDefNode(clazz, returnType, methodName);

		eat(Token.Kind.IDENT);
		eat(Token.Kind.LPAREN);

		if (current.kind != Token.Kind.RPAREN) {
			String paramType = current.text;
			eat(Token.Kind.TYPE);
			String paramName = current.text;
			eat(Token.Kind.IDENT);
			new ParamFuncDefNode(fn, paramType, paramName);
			while (current.kind == Token.Kind.COMMA) {
				eat(Token.Kind.COMMA);
				paramType = current.text;
				eat(Token.Kind.TYPE);
				paramName = current.text;
				eat(Token.Kind.IDENT);
				new ParamFuncDefNode(fn, paramType, paramName);
			}
		}

		eat(Token.Kind.RPAREN);
		block(fn, null, null);
		return fn;
	}

	private boolean isFuncDef() {
		if (current.kind == Token.Kind.TYPE && current.next.kind == Token.Kind.IDENT
				&& current.next.next.kind == Token.Kind.LPAREN)
			return true;
		else return false;
	}

	private Node parseIf() {
		eat(Token.Kind.IF);
		IfNode ifNode = new IfNode();
		currentStatement = ifNode;
		eat(Token.Kind.LPAREN);
		ifNode.cond = expr();
		eat(Token.Kind.RPAREN);
		ifNode.thenBranch = statement();

		List<IfNode> elseIfChain = new ArrayList<>();
		Node elseBranch = null;

		while (current.kind == Token.Kind.ELSE) {
			eat(Token.Kind.ELSE);

			if (current.kind == Token.Kind.IF) {
				IfNode elseIfNode = new IfNode();
				eat(Token.Kind.IF);
				eat(Token.Kind.LPAREN);
				elseIfNode.cond = expr();
				eat(Token.Kind.RPAREN);
				elseIfNode.thenBranch = statement();
				elseIfChain.add(elseIfNode);
			} else {
				elseBranch = statement();
				break;
			}
		}

		// Encaixa os "else if" e "else" como aninhamento de IfNode
		IfNode currentNode = ifNode;

		for (IfNode elif : elseIfChain) {
			currentNode.elseBranch = elif;
			currentNode = elif;
		}

		if (elseBranch != null) currentNode.elseBranch = elseBranch;

		return ifNode;
	}

	private Node parseWhile() {
		eat(Token.Kind.WHILE);
		WhileNode whileNode = new WhileNode();
		currentStatement = whileNode;
		eat(Token.Kind.LPAREN);
		Node cond = expr();
		whileNode.cond = cond;
		eat(Token.Kind.RPAREN);
		whileNode.body = statement();
		return whileNode;
	}

	private Node statement() {

		if (current.kind == Kind.IDENT && current.next.kind == Kind.ASSIGN) {
			Node assign = assign();
			eat(Token.Kind.SEMI);
			return assign;
		}

		if (current.kind == Token.Kind.LBRACE) {
			if (currentStatement instanceof WhileNode w) {
				return block(null, w, null);
			} else if (currentStatement instanceof IfNode i) {
				return block(null, null, i);
			} else {
				throw new RuntimeException("Unexpected block context: " + currentStatement);
			}
		}

		if (current.kind == Token.Kind.TYPE) {
			return varDecl(null, null, currentBlock);
		}

		if (current.kind == Token.Kind.IF) {
			return parseIf();
		}

		if (current.kind == Token.Kind.WHILE) {
			return parseWhile();
		}

		if (current.kind == Token.Kind.RETURN) {
			eat(Token.Kind.RETURN);

			if (current.kind == Token.Kind.SEMI) {
				eat(Token.Kind.SEMI);
				return new ReturnNode(null);
			}

			Node expr = expr();
			if (current.kind == Token.Kind.SEMI) eat(Token.Kind.SEMI);
			return new ReturnNode(expr);
		}

		Node expr = expr();
		if (current.kind == Token.Kind.SEMI) eat(Token.Kind.SEMI);
		return expr;
	}

	private VarDeclNode varDecl(ClassDefNode clazz, FuncDefNode fn, BlockNode block) {
		VarDeclNode varDecl = null;
		String type = current.text;
		eat(Token.Kind.TYPE);

		String name = current.text;
		eat(Token.Kind.IDENT);

		Node value = null;
		if (current.kind == Token.Kind.ASSIGN) {
			eat(Token.Kind.ASSIGN);
			value = expr();
		}

		if (current.kind == Token.Kind.SEMI) eat(Token.Kind.SEMI);

		if (clazz != null) varDecl = new FieldDefNode(clazz, type, name, value);
		else if (fn != null) varDecl = new ParamFuncDefNode(fn, type, name);
		else if (block != null) varDecl = new LocalVarDeclNode(block, type, name, value);
		else throw new RuntimeException("Invalid context for variable declaration");

		return varDecl;
	}

	Node add() {
		Node node = mul();
		while (current.kind == Token.Kind.PLUS || current.kind == Token.Kind.MINUS) {
			Token.Kind op = current.kind;
			eat(op);
			node = new BinOpNode(node, op, mul());
		}
		return node;
	}

	BlockNode block(FuncDefNode fn, WhileNode whileNode, IfNode ifNode) {

		BlockNode block = null;

		if (fn != null) block = new BlockNode(null, fn);
		else if (whileNode != null) block = new BlockNode(currentBlock, whileNode);
		else if (ifNode != null) block = new BlockNode(currentBlock, ifNode);
		else throw new RuntimeException("Invalid context for block");

		// currentBlock is static to avoid passing it as parameter
		// to all statements and expressions functions
		currentBlock = block;

		if (current.kind == Token.Kind.LBRACE) {
			eat(Token.Kind.LBRACE);
			while (current.kind != Token.Kind.RBRACE) {
				block.statements.add(statement());
			}
			eat(Token.Kind.RBRACE);
		} else {
			while (current.kind != Token.Kind.EOF) {
				block.statements.add(statement());
			}
		}

		return block;
	}

	Node compare() {
		Node node = add();
		while (current.kind == Token.Kind.EQ || current.kind == Token.Kind.NEQ || current.kind == Token.Kind.LT
				|| current.kind == Token.Kind.GT || current.kind == Token.Kind.LE || current.kind == Token.Kind.GE) {
			Token.Kind op = current.kind;
			eat(op);
			node = new BinOpNode(node, op, add());
		}
		return node;
	}

	Node expr() {
		Node node = logicalOr();
		if (current.kind == Token.Kind.ASSIGN) {
			eat(Token.Kind.ASSIGN);
			return new AssignNode(node, assign());
		}
		return node;
	}

	Node logicalAnd() {
		Node node = compare();
		while (current.kind == Token.Kind.AND) {
			eat(Token.Kind.AND);
			node = new BinOpNode(node, Token.Kind.AND, compare());
		}
		return node;
	}

	Node logicalOr() {
		Node node = logicalAnd();
		while (current.kind == Token.Kind.OR) {
			eat(Token.Kind.OR);
			node = new BinOpNode(node, Token.Kind.OR, logicalAnd());
		}
		return node;
	}

	Node mul() {
		Node node = unary();
		while (current.kind == Token.Kind.MUL || current.kind == Token.Kind.DIV) {
			Token.Kind op = current.kind;
			eat(op);
			node = new BinOpNode(node, op, primary());
		}
		return node;
	}

	ProgramNode parse() {
		ProgramNode prog = new ProgramNode();

		if (current.kind == Token.Kind.PACKAGE) {
			eat(Token.Kind.PACKAGE);
			StringBuilder sb = new StringBuilder();
			sb.append(current.text);
			eat(Token.Kind.IDENT);

			while (current.kind == Token.Kind.DOT) {
				eat(Token.Kind.DOT);
				sb.append(".");
				sb.append(current.text);
				eat(Token.Kind.IDENT);
			}

			eat(Token.Kind.SEMI);
			prog.packageName = sb.toString();
		}

		if (current.kind == Token.Kind.IMPORT) {
			eat(Token.Kind.IMPORT);
			StringBuilder sb = new StringBuilder();
			sb.append(current.text);
			eat(Token.Kind.IDENT);

			while (current.kind == Token.Kind.DOT) {
				eat(Token.Kind.DOT);
				sb.append(".");
				sb.append(current.text);
				eat(Token.Kind.IDENT);
			}

			eat(Token.Kind.SEMI);
			String imp = sb.toString();
			prog.imports.put(imp, imp);
		}

		while (current.kind != Token.Kind.EOF) {
			if (current.kind == Token.Kind.CLASS) {
				classDef(prog);

			} else if (isFuncDef()) {
				FuncDefNode func = funcDef(null);
				if (!func.name.equals("main")) {
					throw new RuntimeException("Only 'main' is allowed outside of a class. fn:" + func.name);
				}
				if (prog.main != null) {
					throw new RuntimeException("Multiple 'main' functions declared.");
				}
				prog.main = func;
			} else {
				throw new RuntimeException("Expected class or 'main' function");
			}
		}

		// set typeClass for all variables
		for (VarDeclNode v : VarDeclNode.allVars) {
			if (v.typeClass != null) continue;
			ClassDefNode c = prog.types.get(v.type);
			if (c != null && !(v.value instanceof NullNode)) v.typeClass = c;
		}

		// Analyze class dependencies to detect cycles
		ClassDependencyAnalyzer analyzer = new ClassDependencyAnalyzer();
		for (ClassDefNode clazz : prog.types.values()) {
			for (VarDeclNode field : clazz.fields.values()) {
				analyzer.addFieldDependency(clazz.name, field);
			}
		}
		analyzer.checkForCycles();

		return prog;
	}

	Node primary() {
		Node node = null;
		if (current.kind == Token.Kind.NUM) {
			int value = Integer.parseInt(current.text);
			eat(Token.Kind.NUM);
			return new NumNode(value);
		}

		if (current.kind == Token.Kind.NULL) {
			eat(Token.Kind.NULL);
			return new NullNode();
		}

		if (current.kind == Token.Kind.IDENT) {
			String var = current.text;
			VarDeclNode varDecl = currentBlock.findVarDecl(var);
			if (varDecl == null) throw new RuntimeException("Variable not found: " + var);
			node = new IdentNode(currentBlock, varDecl);

			eat(Token.Kind.IDENT);

			while (current.kind == Token.Kind.DOT) {
				eat(Token.Kind.DOT);
				String methodOrField = current.text;
				eat(Token.Kind.IDENT);

				// current token is method call
				if (current.kind == Token.Kind.LPAREN) {
					FuncCallNode fn = new FuncCallNode(currentBlock, varDecl, methodOrField);
					eat(Token.Kind.LPAREN);

					if (current.kind != Token.Kind.RPAREN) {
						fn.args.add(expr());
						while (current.kind == Token.Kind.COMMA) {
							eat(Token.Kind.COMMA);
							fn.args.add(expr());
						}
					}

					eat(Token.Kind.RPAREN);
					node = fn;

				} else {
					// current token is field
					node = new FieldAccessNode(node, methodOrField);
				}
			}

			return node;
		}

		if (current.kind == Token.Kind.LPAREN) {
			eat(Token.Kind.LPAREN);
			Node n = expr();
			eat(Token.Kind.RPAREN);
			return n;
		}

		throw new RuntimeException("Unexpected token: " + current);
	}

	Node unary() {
		if (current.kind == Token.Kind.NOT) {
			eat(Token.Kind.NOT);
			return new UnaryOpNode(Token.Kind.NOT, unary());
		}
		return primary();
	}

}
