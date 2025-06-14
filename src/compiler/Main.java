package compiler;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

class BinOpNode extends Node {
	Node left, right;
	Token.Kind op;

	BinOpNode(Node left, Token.Kind op, Node right) {
		this.left = left;
		this.op = op;
		this.right = right;
	}
}

class CodeGen {
	private final PrintWriter out;

	CodeGen(PrintWriter out) {
		this.out = out;
	}

	private void gen(Node node) {
		if (node instanceof NumNode num) {
			out.println("    mov $" + num.value + ", %rax");
		} else if (node instanceof BinOpNode bin) {
			gen(bin.right);
			out.println("    push %rax");
			gen(bin.left);
			out.println("    pop %rdi");
			switch (bin.op) {
			case PLUS -> out.println("    add %rdi, %rax");
			case MINUS -> out.println("    sub %rdi, %rax");
			case MUL -> out.println("    imul %rdi, %rax");
			case DIV -> {
				out.println("    mov %rax, %rcx");
				out.println("    mov %rdi, %rax");
				out.println("    cqo");
				out.println("    idiv %rcx");
			}
			default -> throw new RuntimeException("Operador não suportado");
			}
		}
	}

	void emit(Node node) {
		out.println(".globl main");
		out.println("main:");
		gen(node);
		out.println("    ret");
	}
}

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

		pos++;
		return switch (c) {
		case '+' -> new Token(Token.Kind.PLUS);
		case '-' -> new Token(Token.Kind.MINUS);
		case '*' -> new Token(Token.Kind.MUL);
		case '/' -> new Token(Token.Kind.DIV);
		case '(' -> new Token(Token.Kind.LPAREN);
		case ')' -> new Token(Token.Kind.RPAREN);
		default -> throw new RuntimeException("Token inválido: " + c);
		};
	}
}

// just to print in console too
class MyPrintWriter extends PrintWriter {

	public MyPrintWriter(String s) throws FileNotFoundException {
		super(s);
	}

	@Override
	public void println(String x) {
		System.out.println(x);
		super.println(x);
	}
}

abstract class Node {
}

class NumNode extends Node {
	int value;

	NumNode(int value) {
		this.value = value;
	}
}

class Parser {
	private final Lexer lexer;
	private Token current;

	Parser(Lexer lexer) {
		this.lexer = lexer;
		this.current = lexer.next();
	}

	private void eat(Token.Kind kind) {
		if (current.kind == kind) current = lexer.next();
		else throw new RuntimeException("Esperado: " + kind);
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

	Node primary() {
		if (current.kind == Token.Kind.NUM) {
			int value = current.value;
			eat(Token.Kind.NUM);
			return new NumNode(value);
		} else if (current.kind == Token.Kind.LPAREN) {
			eat(Token.Kind.LPAREN);
			Node node = expr();
			eat(Token.Kind.RPAREN);
			return node;
		} else {
			throw new RuntimeException("Expressão inválida");
		}
	}
}

class Token {
	enum Kind {
		NUM, PLUS, MINUS, MUL, DIV, LPAREN, RPAREN, EOF
	}

	Kind kind;
	int value; // just for NUM

	Token(Kind kind) {
		this(kind, 0);
	}

	Token(Kind kind, int value) {
		this.kind = kind;
		this.value = value;
	}
}

public class Main {
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Use: java Main \"1 + 2 * 3\"");
			System.exit(1);
		}

		String input = args[0];
		Lexer lexer = new Lexer(input);
		Parser parser = new Parser(lexer);
		Node ast = parser.expr();

		try (PrintWriter out = new MyPrintWriter("out.s")) {
			CodeGen gen = new CodeGen(out);
			gen.emit(ast);
		}
	}
}

