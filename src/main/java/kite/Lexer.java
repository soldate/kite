package kite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class Lexer {
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("type", TokenType.TYPE);
        KEYWORDS.put("void", TokenType.VOID);
        KEYWORDS.put("int", TokenType.INT);
        KEYWORDS.put("uint", TokenType.UINT);
        KEYWORDS.put("long", TokenType.LONG);
        KEYWORDS.put("ulong", TokenType.ULONG);
        KEYWORDS.put("float", TokenType.FLOAT);
        KEYWORDS.put("double", TokenType.DOUBLE);
        KEYWORDS.put("byte", TokenType.BYTE);
        KEYWORDS.put("char", TokenType.CHAR);
        KEYWORDS.put("bool", TokenType.BOOL);
        KEYWORDS.put("string", TokenType.STRING_TYPE);
        KEYWORDS.put("pointer", TokenType.POINTER);
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("for", TokenType.FOR);
    }

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start;
    private int current;
    private int line = 1;
    private int column = 1;
    private int tokenColumn = 1;

    Lexer(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            tokenColumn = column;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(' -> add(TokenType.LEFT_PAREN);
            case ')' -> add(TokenType.RIGHT_PAREN);
            case '{' -> add(TokenType.LEFT_BRACE);
            case '}' -> add(TokenType.RIGHT_BRACE);
            case ';' -> add(TokenType.SEMICOLON);
            case ',' -> add(TokenType.COMMA);
            case '.' -> add(TokenType.DOT);
            case '+' -> add(TokenType.PLUS);
            case '-' -> add(TokenType.MINUS);
            case '*' -> add(TokenType.STAR);
            case '/' -> {
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) {
                        advance();
                    }
                } else {
                    add(TokenType.SLASH);
                }
            }
            case '=' -> add(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
            case '!' -> {
                if (match('=')) {
                    add(TokenType.BANG_EQUAL);
                } else {
                    throw error("Unexpected character '!'");
                }
            }
            case '<' -> add(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
            case '>' -> add(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
            case ' ', '\r', '\t' -> {
            }
            case '\n' -> {
                line++;
                column = 1;
            }
            case '"' -> string();
            default -> {
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    throw error("Unexpected character '" + c + "'");
                }
            }
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }

        String text = source.substring(start, current);
        add(KEYWORDS.getOrDefault(text, TokenType.IDENTIFIER));
    }

    private void number() {
        while (isDigit(peek())) {
            advance();
        }
        add(TokenType.NUMBER);
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
                column = 1;
            }
            advance();
        }

        if (isAtEnd()) {
            throw error("Unterminated string");
        }

        advance();
        add(TokenType.STRING);
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) {
            return false;
        }
        current++;
        column++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(current);
    }

    private char advance() {
        column++;
        return source.charAt(current++);
    }

    private void add(TokenType type) {
        tokens.add(new Token(type, source.substring(start, current), line, tokenColumn));
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private KiteException error(String message) {
        return new KiteException("Line " + line + ", column " + tokenColumn + ": " + message);
    }
}
