package compiler;

//=== TOKENS ===
public class Token {
	public enum Kind {
		NUM, IDENT, INT, RETURN, IF, WHILE, PLUS, MINUS, MUL, DIV, LPAREN, RPAREN, LBRACE, RBRACE, ASSIGN, SEMI, EOF
	}

	Kind kind;
	int value;
	String text;

	Token(Kind kind) {
		this.kind = kind;
	}

	Token(Kind kind, int value) {
		this.kind = kind;
		this.value = value;
	}

	Token(Kind kind, String text) {
		this.kind = kind;
		this.text = text;
	}
}
