package compiler;

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
			return switch (text) {
			case "int" -> new Token(Token.Kind.INT);
			case "return" -> new Token(Token.Kind.RETURN);
			case "if" -> new Token(Token.Kind.IF);
			case "while" -> new Token(Token.Kind.WHILE);
			default -> new Token(Token.Kind.IDENT, text);
			};
		}

		pos++;
		return switch (c) {
		case '+' -> new Token(Token.Kind.PLUS);
		case '-' -> new Token(Token.Kind.MINUS);
		case '*' -> new Token(Token.Kind.MUL);
		case '/' -> new Token(Token.Kind.DIV);
		case '(' -> new Token(Token.Kind.LPAREN);
		case ')' -> new Token(Token.Kind.RPAREN);
		case '{' -> new Token(Token.Kind.LBRACE);
		case '}' -> new Token(Token.Kind.RBRACE);
		case '=' -> new Token(Token.Kind.ASSIGN);
		case ';' -> new Token(Token.Kind.SEMI);
		case ',' -> new Token(Token.Kind.COMMA);
		default -> throw new RuntimeException("Invalid token: " + c);
		};
	}
}