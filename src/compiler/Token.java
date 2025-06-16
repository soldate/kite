package compiler;

//=== TOKENS ===
public class Token {
	public enum Kind {
		NUM, IDENT, PLUS, MINUS, MUL, DIV, EQ, NEQ, LT, GT, LE, GE, ASSIGN, SEMI, COMMA, INT, RETURN, IF, WHILE, LPAREN,
		RPAREN, LBRACE, RBRACE, EOF
	}

	Kind kind;
	int value;
	String text;

	Token(Kind kind, String text, int value) {
		this.kind = kind;
		this.text = text;
		this.value = value;
	}
}
