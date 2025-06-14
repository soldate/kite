// Mini compiler for Kite - Stage 2: local variables and return
package compiler;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BinOpNode extends Node { Node left, right; Token.Kind op;
    BinOpNode(Node left, Token.Kind op, Node right) {
        this.left = left; this.op = op; this.right = right;
    }
}

class BlockNode extends Node { List<Node> statements = new ArrayList<Node>(); }

// === CODEGEN ===
class CodeGen {
    private final PrintWriter out;
    private final Map<String, Integer> vars = new HashMap<String, Integer>();
    private int stackOffset = 0;

    CodeGen(PrintWriter out) {
        this.out = out;
    }

    private void gen(Node node) {
        if (node instanceof BlockNode) {
            BlockNode block = (BlockNode) node;
            for (Node stmt : block.statements) gen(stmt);

        } else if (node instanceof VarDeclNode) {
            VarDeclNode var = (VarDeclNode) node;
            stackOffset += 8;
            int slotOffset = stackOffset + 8; // avoid overwriting pushed %rbp
            vars.put(var.name, slotOffset);
            gen(var.value);
            out.println("    mov %rax, -" + slotOffset + "(%rbp)");

        } else if (node instanceof ReturnNode) {
            ReturnNode ret = (ReturnNode) node;
            gen(ret.expr);
            out.println("    mov %rbp, %rsp");
            out.println("    pop %rbp");
            out.println("    ret");

        } else if (node instanceof NumNode) {
            NumNode num = (NumNode) node;
            out.println("    mov $" + num.value + ", %rax");

        } else if (node instanceof IdentNode) {
            IdentNode id = (IdentNode) node;
            Integer offset = vars.get(id.name);
            if (offset == null) throw new RuntimeException("Undeclared variable: " + id.name);
            out.println("    mov -" + offset + "(%rbp), %rax");

        } else if (node instanceof BinOpNode) {
            BinOpNode bin = (BinOpNode) node;
            gen(bin.right);
            out.println("    push %rax");
            gen(bin.left);
            out.println("    pop %rdi");
            switch (bin.op) {
                case PLUS: out.println("    add %rdi, %rax"); break;
                case MINUS: out.println("    sub %rdi, %rax"); break;
                case MUL: out.println("    imul %rdi, %rax"); break;
                case DIV:
                    out.println("    mov %rax, %rcx");
                    out.println("    mov %rdi, %rax");
                    out.println("    cqo");
                    out.println("    idiv %rcx");
                    break;
                default: throw new RuntimeException("Unsupported operator");
            }
        }
    }

    void emit(Node node) {
        out.println(".globl main");
        out.println("main:");
        out.println("    push %rbp");
        out.println("    mov %rsp, %rbp");
        gen(node);
    }
}

class IdentNode extends Node {
	String name;

	IdentNode(String name) {
		this.name = name;
	}
}

// === LEXER ===
class Lexer {
	private final String input;
	private int pos = 0;

	Lexer(String input) {
		this.input = input;
	}

	Token next() {
		while (pos < input.length() && Character.isWhitespace(input.charAt(pos)))
			pos++;
		if (pos >= input.length()) return new Token(Token.Kind.EOF);

		char c = input.charAt(pos);

		if (Character.isDigit(c)) {
			int start = pos;
			while (pos < input.length() && Character.isDigit(input.charAt(pos)))
				pos++;
			int value = Integer.parseInt(input.substring(start, pos));
			return new Token(Token.Kind.NUM, value);
		}

		if (Character.isLetter(c)) {
			int start = pos;
			while (pos < input.length() && Character.isLetterOrDigit(input.charAt(pos)))
				pos++;
			String text = input.substring(start, pos);
			if ("int".equals(text)) return new Token(Token.Kind.INT);
			if ("return".equals(text)) return new Token(Token.Kind.RETURN);
			return new Token(Token.Kind.IDENT, text);
		}

		pos++;
		switch (c) {
		case '+':
			return new Token(Token.Kind.PLUS);
		case '-':
			return new Token(Token.Kind.MINUS);
		case '*':
			return new Token(Token.Kind.MUL);
		case '/':
			return new Token(Token.Kind.DIV);
		case '(':
			return new Token(Token.Kind.LPAREN);
		case ')':
			return new Token(Token.Kind.RPAREN);
		case '=':
			return new Token(Token.Kind.ASSIGN);
		case ';':
			return new Token(Token.Kind.SEMI);
		default:
			throw new RuntimeException("Invalid token: " + c);
		}
	}
}

// === PRINTWRITER WITH CONSOLE OUTPUT ===
class MyPrintWriter extends PrintWriter {
    public MyPrintWriter(String s) throws FileNotFoundException { super(s); }
    @Override public void println(String x) { System.out.println(x); super.println(x); }
}
// === AST ===
abstract class Node {}
class NumNode extends Node { int value; NumNode(int value) { this.value = value; } }

// === PARSER ===
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

    Node expr() { return add(); }

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
        BlockNode block = new BlockNode();
        while (current.kind != Token.Kind.EOF) {
            block.statements.add(statement());
        }
        return block;
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
        } else {
            throw new RuntimeException("Invalid statement");
        }
    }
}

class ReturnNode extends Node { Node expr; ReturnNode(Node expr) { this.expr = expr; } }

// === TOKENS ===
class Token {
    enum Kind {
        NUM, IDENT, INT, RETURN, PLUS, MINUS, MUL, DIV, LPAREN, RPAREN, ASSIGN, SEMI, EOF
    }

    Kind kind;
    int value;
    String text;

    Token(Kind kind) { this.kind = kind; }
    Token(Kind kind, int value) { this.kind = kind; this.value = value; }
    Token(Kind kind, String text) { this.kind = kind; this.text = text; }
}

class VarDeclNode extends Node {
    String name; String type; Node value;
    VarDeclNode(String type, String name, Node value) {
        this.type = type; this.name = name; this.value = value;
    }
}

// === MAIN ===
public class Main {
	public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java -cp bin compiler.Main \"int x = 5; return x + 2;\"");
            System.exit(1);
        }

        String input = args[0];
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Node ast = parser.parse();

		try (PrintWriter out = new MyPrintWriter("out.s")) {
			CodeGen gen = new CodeGen(out);
			gen.emit(ast);
		}
	}
}
