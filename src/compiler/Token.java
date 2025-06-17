package compiler;

//=== TOKENS ===
public class Token {
	public enum Kind {
		NUM, IDENT, PLUS, MINUS, MUL, DIV, EQ, NEQ, LT, GT, LE, GE, ASSIGN, SEMI, COMMA, VOID, INT, BOOL, RETURN, IF,
		WHILE,
		LPAREN,
		RPAREN, LBRACE, RBRACE, EOF, TRUE, FALSE
	}

	Kind kind;
	int value;
	String text;

	Token(Kind kind) {
		this.kind = kind;
	}

	Token(Kind kind, String text, int value) {
		this.kind = kind;
		this.text = text;
		this.value = value;
	}

	@Override
	public String toString() {
		return "token=(kind:" + kind + ", value:" + value + ", text:" + text + ")";
	}

}
