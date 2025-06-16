package compiler;

import java.util.ArrayList;
import java.util.List;

import compiler.Token.Kind;
import compiler.ast.*;

//=== PARSER ===
class Parser {
	private final Lexer lexer;
	private Token current;

	Parser(Lexer lexer) {
		this.lexer = lexer;
		this.current = lexer.nextToken();
	}

	private void eat(Token.Kind kind) {
		if (current.kind == kind) current = lexer.nextToken();
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
	    return compare();
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
		BlockNode program = new BlockNode();
		while (current.kind != Token.Kind.EOF) {
			program.statements.add(statement());
		}
		return program;
	}

	Node primary() {
		if (current.kind == Token.Kind.NUM) {
			int value = current.value;
			eat(Token.Kind.NUM);
			return new NumNode(value);
		} else if (current.kind == Token.Kind.IDENT) {
			String name = current.text;
			eat(Token.Kind.IDENT);

			if (current.kind == Token.Kind.LPAREN) {
				eat(Token.Kind.LPAREN);
				List<Node> args = new ArrayList<>();
				if (current.kind != Token.Kind.RPAREN) {
					args.add(expr());
					while (current.kind == Token.Kind.COMMA) {
						eat(Token.Kind.COMMA);
						args.add(expr());
					}
				}
				eat(Token.Kind.RPAREN);
				return new FuncCallNode(name, args);
			}

			return new IdentNode(name);

		} else if (current.kind == Token.Kind.LPAREN) {
			eat(Token.Kind.LPAREN);
			Node node = expr();
			eat(Token.Kind.RPAREN);
			return node;

		} else if (current.kind == Token.Kind.TRUE || current.kind == Token.Kind.FALSE) {
			boolean val = current.kind == Token.Kind.TRUE;
			eat(current.kind);
			return new NumNode(val ? 1 : 0);

		} else {
			throw new RuntimeException("Invalid expression");
		}
	}

	Node statement() {
		if (current.kind == Token.Kind.INT || current.kind == Token.Kind.BOOL) {
			Token.Kind type = current.kind;
			eat(type);
			String name = current.text;
			eat(Token.Kind.IDENT);

			if (current.kind == Token.Kind.LPAREN) {
				eat(Token.Kind.LPAREN);
				List<String> params = new ArrayList<>();
				if (current.kind != Token.Kind.RPAREN) {
					eat(type);
					String paramName = current.text;
					eat(Token.Kind.IDENT);
					params.add(paramName);
					while (current.kind == Token.Kind.COMMA) {
						eat(Token.Kind.COMMA);
						eat(type);
						paramName = current.text;
						eat(Token.Kind.IDENT);
						params.add(paramName);
					}
				}
				eat(Token.Kind.RPAREN);
				Node body = statement();
				return new FuncDefNode(name, params, (BlockNode) body);
			} else {
				Node expr = expr();
				eat(Token.Kind.SEMI);
				return new VarDeclNode(type.name().toLowerCase(), name, expr); // "int" ou "bool"
			}
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
            if (current.kind == Token.Kind.LPAREN) {
                eat(Token.Kind.LPAREN);
                List<Node> args = new ArrayList<>();
                if (current.kind != Token.Kind.RPAREN) {
                    args.add(expr());
                    while (current.kind == Token.Kind.COMMA) {
                        eat(Token.Kind.COMMA);
                        args.add(expr());
                    }
                }
                eat(Token.Kind.RPAREN);
                eat(Token.Kind.SEMI);
                return new FuncCallNode(name, args);
            } else {
                eat(Token.Kind.ASSIGN);
                Node value = expr();
                eat(Token.Kind.SEMI);
                return new AssignNode(name, value);
            }

        } else {
            throw new RuntimeException("Invalid statement");
        }
	}
}
