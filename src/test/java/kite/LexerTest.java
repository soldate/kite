package kite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

final class LexerTest {
    @Test
    void recognizesKiteKeywordsAndPrimitiveTypes() {
        List<TokenType> types = new Lexer("special type bool true false uint long string list pointer array stack delete").scanTokens()
                .stream()
                .map(Token::type)
                .toList();

        assertEquals(List.of(
                TokenType.SPECIAL,
                TokenType.TYPE,
                TokenType.BOOL,
                TokenType.TRUE,
                TokenType.FALSE,
                TokenType.UINT,
                TokenType.LONG,
                TokenType.STRING_TYPE,
                TokenType.LIST,
                TokenType.POINTER,
                TokenType.ARRAY,
                TokenType.STACK,
                TokenType.DELETE,
                TokenType.EOF), types);
    }
}
