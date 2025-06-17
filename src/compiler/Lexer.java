package compiler;

class Lexer {
	private final String input;
	private int pos = 0;

	Lexer(String input) {
		this.input = input;
	}

	private char next() {
		return pos < input.length() ? input.charAt(pos++) : '\0';
	}

	private char peek() {
		return pos < input.length() ? input.charAt(pos) : '\0';
	}

	Token nextToken() {
        while (Character.isWhitespace(peek())) next();
        char c = peek();
        if (Character.isDigit(c)) {
            int val = 0;
            while (Character.isDigit(peek())) val = val * 10 + (next() - '0');
            return new Token(Token.Kind.NUM, "", val);
        } else if (Character.isLetter(c)) {
            StringBuilder sb = new StringBuilder();
            while (Character.isLetterOrDigit(peek())) sb.append(next());
            String word = sb.toString();
            return switch (word) {
                case "int" -> new Token(Token.Kind.INT, word, 0);
			case "bool" -> new Token(Token.Kind.BOOL, word, 0);
			case "true" -> new Token(Token.Kind.TRUE, word, 1);
			case "false" -> new Token(Token.Kind.FALSE, word, 0);
                case "return" -> new Token(Token.Kind.RETURN, word, 0);
			case "void" -> new Token(Token.Kind.VOID);
                case "if" -> new Token(Token.Kind.IF, word, 0);
                case "while" -> new Token(Token.Kind.WHILE, word, 0);
                default -> new Token(Token.Kind.IDENT, word, 0);
            };
        } else {
            switch (next()) {
                case '+' -> {return new Token(Token.Kind.PLUS, "+", 0);}
                case '-' -> {return new Token(Token.Kind.MINUS, "-", 0);}
                case '*' -> {return new Token(Token.Kind.MUL, "*", 0);}
                case '/' -> {return new Token(Token.Kind.DIV, "/", 0);}
                case '(' -> {return new Token(Token.Kind.LPAREN, "(", 0);}
                case ')' -> {return new Token(Token.Kind.RPAREN, ")", 0);}
                case '{' -> {return new Token(Token.Kind.LBRACE, "{", 0);}
                case '}' -> {return new Token(Token.Kind.RBRACE, "}", 0);}
                case ',' -> {return new Token(Token.Kind.COMMA, ",", 0);}
                case ';' -> {return new Token(Token.Kind.SEMI, ";", 0);}
                case '=' -> {
                    if (peek() == '=') {
                        next(); return new Token(Token.Kind.EQ, "==", 0);
                    } else return new Token(Token.Kind.ASSIGN, "=", 0);
                }
                case '!' -> {
                    if (peek() == '=') {
                        next(); return new Token(Token.Kind.NEQ, "!=", 0);
                    } else throw new RuntimeException("Unexpected: !");
                }
                case '<' -> {
                    if (peek() == '=') {
                        next(); return new Token(Token.Kind.LE, "<=", 0);
                    } else return new Token(Token.Kind.LT, "<", 0);
                }
                case '>' -> {
                    if (peek() == '=') {
                        next(); return new Token(Token.Kind.GE, ">=", 0);
                    } else return new Token(Token.Kind.GT, ">", 0);
                }
			case '\0' -> {
				return new Token(Token.Kind.EOF, "", 0);
			}
                default -> throw new RuntimeException("Unexpected: " + c);
            }
        }
    }
}