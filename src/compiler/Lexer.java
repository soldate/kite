
package compiler;

import java.util.*;

import compiler.Token.*;

public class Lexer {
	static int pos = 0;
	private final List<Token> tokens = new ArrayList<>();
	private final Set<String> keywords = Set.of("package", "import", "class", "return", "if", "else", "while", "true",
			"false", "null");
	private final Set<String> builtInTypes = Set.of("int", "void"); // , "bool", "char", "float", "double"

	public Lexer(String input) {
		tokenize(input);
	}

	private void addToken(Token token) {
		if (!tokens.isEmpty()) {
			Token last = tokens.get(tokens.size() - 1);
			last.next = token;
			token.prev = last;

			// If the last token was an IDENT, and the current is also an IDENT,
			// we treat the last as a TYPE.
			if (token.kind == Kind.IDENT && last.kind == Kind.IDENT) {
				last.kind = Kind.TYPE;
			}
		}
		tokens.add(token);
	}

	private void tokenize(String input) {
		int p = 0;
		while (p < input.length()) {
			char ch = input.charAt(p);

			if (ch == '/' && p + 1 < input.length()) {
				char next = input.charAt(p + 1);
				if (next == '/') {
					while (p < input.length() && input.charAt(p) != '\n')
						p++;
					continue;
				} else if (next == '*') {
					p += 2;
					while (p + 1 < input.length() && !(input.charAt(p) == '*' && input.charAt(p + 1) == '/'))
						p++;
					p += 2;
					continue;
				}
			}

			if (Character.isWhitespace(ch)) {
				p++;
				continue;
			}

			if (Character.isDigit(ch)) {
				int start = p;
				while (p < input.length() && Character.isDigit(input.charAt(p)))
					p++;
				String num = input.substring(start, p);
				addToken(new Token(Kind.NUM, num));
				continue;
			}

			if (Character.isLetter(ch)) {
				int start = p;
				while (p < input.length() && Character.isLetterOrDigit(input.charAt(p)))
					p++;
				String word = input.substring(start, p);
				if (keywords.contains(word)) {
					addToken(new Token(word));
				} else if (builtInTypes.contains(word)) {
					addToken(new Token(Kind.TYPE, word));
				} else {
					addToken(new Token(Kind.IDENT, word));
				}
				continue;
			}

			// Two-character operators
			if (p + 1 < input.length()) {
				String op2 = input.substring(p, p + 2);
				boolean find = true;
				Kind kind = null;
				switch (op2) {
				case "==":
					kind = Kind.EQ;
					break;
				case "!=":
					kind = Kind.NEQ;
					break;
				case "<=":
					kind = Kind.LE;
					break;
				case ">=":
					kind = Kind.GE;
					break;
				case "&&":
					kind = Kind.AND;
					break;
				case "||":
					kind = Kind.OR;
					break;
				default:
					find = false;
					break;
				}
				if (find) {
					addToken(new Token(kind, op2));
					p += 2;
					continue;
				}
			}

			// Single-character tokens
			char c = input.charAt(p++);
			switch (c) {
			case '+' -> addToken(new Token(Kind.PLUS, "+"));
			case '-' -> addToken(new Token(Kind.MINUS, "-"));
			case '*' -> addToken(new Token(Kind.MUL, "*"));
			case '/' -> addToken(new Token(Kind.DIV, "/"));
			case '=' -> addToken(new Token(Kind.ASSIGN, "="));
			case '<' -> addToken(new Token(Kind.LT, "<"));
			case '>' -> addToken(new Token(Kind.GT, ">"));
			case '(' -> addToken(new Token(Kind.LPAREN, "("));
			case ')' -> addToken(new Token(Kind.RPAREN, ")"));
			case '{' -> addToken(new Token(Kind.LBRACE, "{"));
			case '}' -> addToken(new Token(Kind.RBRACE, "}"));
			case ',' -> addToken(new Token(Kind.COMMA, ","));
			case ';' -> addToken(new Token(Kind.SEMI, ";"));
			case '.' -> addToken(new Token(Kind.DOT, "."));
			case '!' -> addToken(new Token(Kind.NOT, "!"));
			default -> throw new RuntimeException("Unknown character: " + c);
			}
		}

		// EOF
		addToken(new Token(Kind.EOF, ""));
	}

	public void addType(String name) {
		builtInTypes.add(name);
	}

	public Token advance() {
		if (pos < tokens.size() - 1) pos++;
		return current();
	}

	public Token current() {
		return tokens.get(pos);
	}

	public Token rewind() {
		if (pos > 0) pos--;
		return current();
	}
}
