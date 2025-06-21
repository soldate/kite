
package compiler;

import java.util.*;

import compiler.Token.Kind;
import compiler.ast.*;

class Parser {
    private final Lexer lexer;
    private Token current;
    private final Map<String, StructDefNode> structDefs = new HashMap<>();

    Parser(Lexer lexer) {
        this.lexer = lexer;
        this.current = lexer.current();
    }

    private void eat(Token.Kind kind) {
        if (current.kind == kind) {
            current = lexer.advance();
        } else {
            throw new RuntimeException("Expected: " + kind + ", but found: " + current.kind + " (" + current.text + ")");
        }
    }

    Node parse() {
        BlockNode program = new BlockNode();
        while (current.kind != Token.Kind.EOF) {
            program.statements.add(statement());
        }
        return program;
    }

	private FuncDefNode funcDef() {
		String returnType = current.text;
		eat(Token.Kind.TYPE);
	
		String name = current.text;
		eat(Token.Kind.IDENT);
	
		eat(Token.Kind.LPAREN);
	
		List<String> params = new ArrayList<>();
		if (current.kind != Token.Kind.RPAREN) {
			do {
				String paramType = current.text;
				eat(Token.Kind.TYPE);
	
				String paramName = current.text;
				eat(Token.Kind.IDENT);
	
				params.add(paramName);
	
				if (current.kind == Token.Kind.COMMA) {
					eat(Token.Kind.COMMA);
				} else {
					break;
				}
			} while (true);
		}
	
		eat(Token.Kind.RPAREN); 	
		BlockNode body = (BlockNode) block(); 
	
		return new FuncDefNode(name, params, body, returnType);
	}

	private FuncCallNode funcCall() {
		String name = current.text;
		eat(Token.Kind.LPAREN);
	
		List<Node> args = new ArrayList<>();
		if (current.kind != Token.Kind.RPAREN) {
			do {
				Node arg = expr();
				args.add(arg);
	
				if (current.kind == Token.Kind.COMMA) {
					eat(Token.Kind.COMMA);
				} else {
					break;
				}
			} while (true);
		}
	
		eat(Token.Kind.RPAREN);
	
		return new FuncCallNode(name, args);
	}
	
	
    private Node statement() {

		if (current.kind == Kind.IDENT) {
			if (current.next.kind == Kind.LPAREN) return funcCall();
			else if (current.next.kind == Kind.ASSIGN) return assign();
		} 

        if (current.kind == Token.Kind.STRUCT) {
            StructDefNode def = structDef();
            structDefs.put(def.name, def);
            return def;
        }

		if (current.kind == Token.Kind.TYPE) {
			if (current.next.next.kind == Kind.LPAREN) {
				return funcDef();
			} else return varDecl();
		}

        if (current.kind == Token.Kind.LBRACE) {
            return block();
        }

        if (current.kind == Token.Kind.RETURN) {
            eat(Token.Kind.RETURN);
            if (current.kind == Token.Kind.SEMI) {
                eat(Token.Kind.SEMI);
                return new ReturnNode(null);
            } else {
                Node expr = expr();
                eat(Token.Kind.SEMI);
                return new ReturnNode(expr);
            }
        }

        Node expr = expr();
        if (current.kind == Token.Kind.SEMI)
            eat(Token.Kind.SEMI);
        return expr;
    }

    private StructDefNode structDef() {
        eat(Token.Kind.STRUCT);
        String name = current.text;
        eat(Token.Kind.IDENT);
        eat(Token.Kind.LBRACE);

        List<VarDeclNode> fields = new ArrayList<>();
        while (current.kind != Token.Kind.RBRACE) {
            fields.add(varDecl());
        }

        eat(Token.Kind.RBRACE);
        return new StructDefNode(name, fields);
    }

    private VarDeclNode varDecl() {
        String type = current.text;
        eat(Token.Kind.TYPE); // tipo (ex: int ou Point)

        String name = current.text;
        eat(Token.Kind.IDENT); // nome da variável

        Node value = null;
        if (current.kind == Token.Kind.ASSIGN) {
            eat(Token.Kind.ASSIGN);
            value = expr();
        }

        if (current.kind == Token.Kind.SEMI)
            eat(Token.Kind.SEMI);

        return new VarDeclNode(type, name, value);
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
        return assign();
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

    Node compare() {
        Node node = add();
        while (current.kind == Token.Kind.EQ || current.kind == Token.Kind.NEQ ||
               current.kind == Token.Kind.LT || current.kind == Token.Kind.GT ||
               current.kind == Token.Kind.LE || current.kind == Token.Kind.GE) {
            Token.Kind op = current.kind;
            eat(op);
            node = new BinOpNode(node, op, add());
        }
        return node;
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

    Node mul() {
        Node node = primary();
        while (current.kind == Token.Kind.MUL || current.kind == Token.Kind.DIV) {
            Token.Kind op = current.kind;
            eat(op);
            node = new BinOpNode(node, op, primary());
        }
        return node;
    }

    Node primary() {
        if (current.kind == Token.Kind.NUM) {
            int value = Integer.parseInt(current.text);
            eat(Token.Kind.NUM);
            return new NumNode(value);
        }

		if (current.kind == Token.Kind.IDENT) {
			Node node = new IdentNode(current.text);
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
				return new FuncCallNode(((IdentNode) node).name, args);
			}
		
			while (current.kind == Token.Kind.DOT) {
				eat(Token.Kind.DOT);
				String field = current.text;
				eat(Token.Kind.IDENT);
				node = new FieldAccessNode(node, field);
			}
		
			return node;
		}

        if (current.kind == Token.Kind.LPAREN) {
            eat(Token.Kind.LPAREN);
            Node node = expr();
            eat(Token.Kind.RPAREN);
            return node;
        }

        throw new RuntimeException("Unexpected token: " + current.kind);
    }
}
