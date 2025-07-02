package compiler;

public class Token {
	public enum Kind {
		IDENT, NUM, PLUS, MINUS, MUL, DIV, EQ, NEQ, LT, GT, LE, GE, ASSIGN, SEMI, COMMA, TYPE, RETURN, IF, ELSE,
		WHILE, LPAREN, RPAREN, LBRACE, RBRACE, EOF, TRUE, FALSE, CLASS, DOT, AND, OR, NOT, THIS, NULL
	}

	public Kind kind;
	public int value;
	public String text;

    public Token next;
    public Token prev;
	
	// position in the input string
	// for debugging purposes
	public int pos;

	Token(Kind kind, String text) {
		this.kind = kind;
		this.text = text;
		
		try {
			this.value = Integer.parseInt(text);	
		} catch (NumberFormatException e) {			
		}		

		// for error messages, we need to know the position in the input string
		this.pos = Lexer.pos - text.length();

	}

	Token(String word) {
		this(Kind.valueOf(word.toUpperCase()), word);
	}

	@Override
	public String toString() {
		return "token=(kind:" + kind + ", value:" + value + ", text:" + text + ")";
	}

}
