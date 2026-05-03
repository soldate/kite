package kite;

import java.util.ArrayList;
import java.util.List;

import kite.Ast.Assign;
import kite.Ast.ArrayNew;
import kite.Ast.Binary;
import kite.Ast.Call;
import kite.Ast.DeleteStmt;
import kite.Ast.Expr;
import kite.Ast.ExprStmt;
import kite.Ast.FieldDecl;
import kite.Ast.ForStmt;
import kite.Ast.Get;
import kite.Ast.IfStmt;
import kite.Ast.Index;
import kite.Ast.Literal;
import kite.Ast.Member;
import kite.Ast.MethodDecl;
import kite.Ast.Param;
import kite.Ast.Program;
import kite.Ast.ReturnStmt;
import kite.Ast.Stmt;
import kite.Ast.TypeDecl;
import kite.Ast.VarDecl;
import kite.Ast.Variable;
import kite.Ast.WhileStmt;

final class Parser {
    private final List<Token> tokens;
    private int current;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Program parse() {
        List<TypeDecl> types = new ArrayList<>();
        while (!isAtEnd()) {
            types.add(typeDecl());
        }
        return new Program(types);
    }

    private TypeDecl typeDecl() {
        consume(TokenType.TYPE, "Expected 'type'");
        String name = consume(TokenType.IDENTIFIER, "Expected type name").lexeme();
        consume(TokenType.LEFT_BRACE, "Expected '{' after type name");

        List<Member> members = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            String memberType = parseType();
            String memberName = consume(TokenType.IDENTIFIER, "Expected member name").lexeme();
            if (match(TokenType.LEFT_PAREN)) {
                members.add(methodDecl(memberType, memberName));
            } else {
                consume(TokenType.SEMICOLON, "Expected ';' after field");
                members.add(new FieldDecl(memberType, memberName));
            }
        }

        consume(TokenType.RIGHT_BRACE, "Expected '}' after type body");
        return new TypeDecl(name, members);
    }

    private MethodDecl methodDecl(String returnType, String name) {
        List<Param> params = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                String type = parseType();
                String paramName = consume(TokenType.IDENTIFIER, "Expected parameter name").lexeme();
                params.add(new Param(type, paramName));
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters");
        List<Stmt> body = block("Expected '{' before method body", "Expected '}' after method body");
        return new MethodDecl(returnType, name, params, body);
    }

    private Stmt statement() {
        if (match(TokenType.IF)) {
            consume(TokenType.LEFT_PAREN, "Expected '(' after 'if'");
            Expr condition = expression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after if condition");
            List<Stmt> thenBranch = block("Expected '{' before if body", "Expected '}' after if body");
            List<Stmt> elseBranch = List.of();
            if (match(TokenType.ELSE)) {
                elseBranch = block("Expected '{' before else body", "Expected '}' after else body");
            }
            return new IfStmt(condition, thenBranch, elseBranch);
        }

        if (match(TokenType.WHILE)) {
            consume(TokenType.LEFT_PAREN, "Expected '(' after 'while'");
            Expr condition = expression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after while condition");
            List<Stmt> body = block("Expected '{' before while body", "Expected '}' after while body");
            return new WhileStmt(condition, body);
        }

        if (match(TokenType.FOR)) {
            consume(TokenType.LEFT_PAREN, "Expected '(' after 'for'");
            Stmt initializer = null;
            if (match(TokenType.SEMICOLON)) {
                initializer = null;
            } else if (isVarDeclStart()) {
                initializer = varDecl();
            } else {
                Expr expr = expression();
                consume(TokenType.SEMICOLON, "Expected ';' after for initializer");
                initializer = new ExprStmt(expr);
            }

            Expr condition = null;
            if (!check(TokenType.SEMICOLON)) {
                condition = expression();
            }
            consume(TokenType.SEMICOLON, "Expected ';' after for condition");

            Expr increment = null;
            if (!check(TokenType.RIGHT_PAREN)) {
                increment = expression();
            }
            consume(TokenType.RIGHT_PAREN, "Expected ')' after for clauses");

            List<Stmt> body = block("Expected '{' before for body", "Expected '}' after for body");
            return new ForStmt(initializer, condition, increment, body);
        }

        if (match(TokenType.RETURN)) {
            Expr value = null;
            if (!check(TokenType.SEMICOLON)) {
                value = expression();
            }
            consume(TokenType.SEMICOLON, "Expected ';' after return");
            return new ReturnStmt(value);
        }

        if (match(TokenType.DELETE)) {
            Expr expr = expression();
            consume(TokenType.SEMICOLON, "Expected ';' after delete");
            return new DeleteStmt(expr);
        }

        if (isVarDeclStart()) {
            return varDecl();
        }

        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Expected ';' after expression");
        return new ExprStmt(expr);
    }

    private VarDecl varDecl() {
        boolean heap = match(TokenType.HEAP);
        String type = parseType();
        String name = consume(TokenType.IDENTIFIER, "Expected variable name").lexeme();
        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }
        consume(TokenType.SEMICOLON, "Expected ';' after variable declaration");
        return new VarDecl(type, name, initializer, heap);
    }

    private List<Stmt> block(String openMessage, String closeMessage) {
        consume(TokenType.LEFT_BRACE, openMessage);
        List<Stmt> body = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            body.add(statement());
        }
        consume(TokenType.RIGHT_BRACE, closeMessage);
        return body;
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = equality();
        if (match(TokenType.EQUAL)) {
            Expr value = assignment();
            return new Assign(expr, value);
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
            String operator = previous().lexeme();
            Expr right = comparison();
            expr = new Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(TokenType.LESS, TokenType.LESS_EQUAL, TokenType.GREATER, TokenType.GREATER_EQUAL)) {
            String operator = previous().lexeme();
            Expr right = term();
            expr = new Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            String operator = previous().lexeme();
            Expr right = factor();
            expr = new Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = call();
        while (match(TokenType.STAR, TokenType.SLASH)) {
            String operator = previous().lexeme();
            Expr right = call();
            expr = new Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                List<Expr> args = new ArrayList<>();
                if (!check(TokenType.RIGHT_PAREN)) {
                    do {
                        args.add(expression());
                    } while (match(TokenType.COMMA));
                }
                consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments");
                expr = new Call(expr, args);
            } else if (match(TokenType.DOT)) {
                String name = consume(TokenType.IDENTIFIER, "Expected property name after '.'").lexeme();
                expr = new Get(expr, name);
            } else if (match(TokenType.LEFT_BRACKET)) {
                Expr index = expression();
                consume(TokenType.RIGHT_BRACKET, "Expected ']' after index");
                expr = new Index(expr, index);
            } else {
                break;
            }
        }
        return expr;
    }

    private Expr primary() {
        if (match(TokenType.NUMBER, TokenType.STRING, TokenType.TRUE, TokenType.FALSE)) {
            return new Literal(previous().lexeme());
        }
        if (match(TokenType.IDENTIFIER)) {
            return new Variable(previous().lexeme());
        }
        if (match(TokenType.ARRAY)) {
            String elementType = parseType();
            consume(TokenType.LEFT_PAREN, "Expected '(' after array element type");
            Expr size = expression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after array size");
            return new ArrayNew(elementType, size);
        }
        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression");
            return expr;
        }
        throw error(peek(), "Expected expression");
    }

    private String parseType() {
        if (match(TokenType.POINTER)) {
            return "pointer " + parseType();
        }
        String type;
        if (match(TokenType.VOID, TokenType.INT, TokenType.UINT, TokenType.LONG, TokenType.ULONG,
                TokenType.FLOAT, TokenType.DOUBLE, TokenType.BYTE, TokenType.CHAR, TokenType.BOOL,
                TokenType.STRING_TYPE, TokenType.IDENTIFIER)) {
            type = previous().lexeme();
            while (match(TokenType.LEFT_BRACKET)) {
                consume(TokenType.RIGHT_BRACKET, "Expected ']' after array type");
                type += "[]";
            }
            return type;
        }
        throw error(peek(), "Expected type");
    }

    private boolean isTypeStart(TokenType type) {
        return type == TokenType.VOID || type == TokenType.INT || type == TokenType.UINT || type == TokenType.LONG
                || type == TokenType.ULONG || type == TokenType.FLOAT || type == TokenType.DOUBLE
                || type == TokenType.BYTE || type == TokenType.CHAR || type == TokenType.BOOL
                || type == TokenType.STRING_TYPE || type == TokenType.POINTER || type == TokenType.IDENTIFIER;
    }

    private boolean isVarDeclStart() {
        if (check(TokenType.HEAP)) {
            return true;
        }
        if (check(TokenType.POINTER)) {
            return true;
        }
        if (!isTypeStart(peek().type())) {
            return false;
        }
        if (checkNext(TokenType.IDENTIFIER)) {
            return true;
        }
        return current + 3 < tokens.size()
                && tokens.get(current + 1).type() == TokenType.LEFT_BRACKET
                && tokens.get(current + 2).type() == TokenType.RIGHT_BRACKET
                && tokens.get(current + 3).type() == TokenType.IDENTIFIER;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().type() == type;
    }

    private boolean checkNext(TokenType type) {
        if (current + 1 >= tokens.size()) {
            return false;
        }
        return tokens.get(current + 1).type() == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private KiteException error(Token token, String message) {
        return new KiteException("Line " + token.line() + ", column " + token.column() + ": " + message);
    }
}
