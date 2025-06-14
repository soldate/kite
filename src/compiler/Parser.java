package compiler;

import compiler.Token.Kind;
import compiler.ast.AssignNode;
import compiler.ast.BinOpNode;
import compiler.ast.BlockNode;
import compiler.ast.IdentNode;
import compiler.ast.IfNode;
import compiler.ast.Node;
import compiler.ast.NumNode;
import compiler.ast.ReturnNode;
import compiler.ast.VarDeclNode;
import compiler.ast.WhileNode;

//=== PARSER ===
class Parser {
	private final Lexer lexer;
	private Token current;

	Parser(Lexer lexer) {
		this.lexer = lexer;
		this.current = lexer.next();
	}

	private void eat(Token.Kind kind) {
		if (current.kind == kind) current = lexer.next();
		else throw new RuntimeException("Expected: " + kind);
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

	Node block() {
		BlockNode block = new BlockNode();
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

	Node expr() {
		return add();
	}

	Node mul() {
		Node node = primary();
		while (current.kind == Token.Kind.MUL || current.kind == Token.Kind.DIV) {
			Token.Kind op = current.kind;
			eat(op);
			node = new BinOpNode(node, op, primary());
		}
		return node;
	}

	Node parse() {
		return block();
	}

	Node primary() {
     if (current.kind == Token.Kind.NUM) {
         int value = current.value;
         eat(Token.Kind.NUM);
         return new NumNode(value);
     } else if (current.kind == Token.Kind.IDENT) {
         String name = current.text;
         eat(Token.Kind.IDENT);
         return new IdentNode(name);
     } else if (current.kind == Token.Kind.LPAREN) {
         eat(Token.Kind.LPAREN);
         Node node = expr();
         eat(Token.Kind.RPAREN);
         return node;
     } else {
         throw new RuntimeException("Invalid expression");
     }
 }

	Node statement() {
     if (current.kind == Token.Kind.INT) {
         eat(Token.Kind.INT);
         String name = current.text;
         eat(Token.Kind.IDENT);
         eat(Token.Kind.ASSIGN);
         Node expr = expr();
         eat(Token.Kind.SEMI);
         return new VarDeclNode("int", name, expr);

     } else if (current.kind == Token.Kind.RETURN) {
         eat(Token.Kind.RETURN);
         Node expr = expr();
         eat(Token.Kind.SEMI);
         return new ReturnNode(expr);

     } else if (current.kind == Token.Kind.IF) {
         eat(Token.Kind.IF);
         eat(Token.Kind.LPAREN);
         Node cond = expr();
         eat(Token.Kind.RPAREN);
         Node thenBranch = statement();
         return new IfNode(cond, thenBranch);

     } else if (current.kind == Token.Kind.WHILE) {
         eat(Token.Kind.WHILE);
         eat(Token.Kind.LPAREN);
         Node cond = expr();
         eat(Token.Kind.RPAREN);
         Node body = statement();
         return new WhileNode(cond, body);

     } else if (current.kind == Token.Kind.LBRACE) {
         return block();

     } else if (current.kind == Token.Kind.IDENT) {
			String name = current.text;
			eat(Token.Kind.IDENT);
			if (current.kind == Token.Kind.ASSIGN) {
				eat(Token.Kind.ASSIGN);
				Node value = expr();
				eat(Token.Kind.SEMI);
				return new AssignNode(name, value);
			} else {
				throw new RuntimeException("Expected '=' after identifier: " + name);
			}
		} else {
         throw new RuntimeException("Invalid statement");
     }
 }
}
