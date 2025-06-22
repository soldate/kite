package compiler;

public class Token {
	public enum Kind {
		NUM, IDENT, PLUS, MINUS, MUL, DIV, EQ, NEQ, LT, GT, LE, GE, ASSIGN, SEMI, COMMA, VOID, TYPE, BOOL, RETURN, IF, ELSE,
		WHILE, LPAREN, RPAREN, LBRACE, RBRACE, EOF, TRUE, FALSE, STRUCT, DOT, AND, OR, NOT
	}

	Kind kind;
	int value;
	String text;

    Token next;
    Token prev;	

	Token(Kind kind) {
		this.kind = kind;
	}

	Token(Kind kind, String text) {
		this.kind = kind;
		this.text = text;
		try {
			this.value = Integer.parseInt(text);	
		} catch (NumberFormatException e) {			
		}		
	}

	Token(String word) {
		this(Kind.valueOf(word.toUpperCase()));
	}

	@Override
	public String toString() {
		return "token=(kind:" + kind + ", value:" + value + ", text:" + text + ")";
	}

}
