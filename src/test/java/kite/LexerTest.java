package kite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

final class LexerTest {
    @Test
    void recognizesKiteKeywordsAndPrimitiveTypes() {
        List<TokenType> types = new Lexer("type bool true false uint long string pointer array").scanTokens()
                .stream()
                .map(Token::type)
                .toList();

        assertEquals(List.of(
                TokenType.TYPE,
                TokenType.BOOL,
                TokenType.TRUE,
                TokenType.FALSE,
                TokenType.UINT,
                TokenType.LONG,
                TokenType.STRING_TYPE,
                TokenType.POINTER,
                TokenType.ARRAY,
                TokenType.EOF), types);
    }
}
