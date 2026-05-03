package kite;

import java.util.List;

final class Compiler {
    String compile(String source) {
        List<Token> tokens = new Lexer(source).scanTokens();
        Ast.Program program = new Parser(tokens).parse();
        return new CGenerator().generate(program);
    }
}
