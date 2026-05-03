package kite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

final class CompilerTest {
    private final Compiler compiler = new Compiler();

    @Test
    void compilesHelloExampleToC() throws IOException {
        String c = compileExample("hello.kite");

        assertEquals("""
                #include <stdint.h>
                #include <stdbool.h>
                #include <stdio.h>

                static void console_write(const char* text) { printf("%s", text); }

                typedef struct kite_main {
                } kite_main;

                int main(void) {
                    console_write("hello");
                    return 0;
                }

                """, c);
    }

    @Test
    void compilesFieldAccessThroughImplicitSelf() throws IOException {
        String c = compileExample("node.kite");

        assertTrue(c.contains("typedef struct kite_node {"));
        assertTrue(c.contains("int32_t value;"));
        assertTrue(c.contains("void node_init(kite_node* self, int32_t v)"));
        assertTrue(c.contains("self->value = v;"));
    }

    @Test
    void compilesControlFlow() throws IOException {
        String c = compileExample("control.kite");

        assertTrue(c.contains("while (i < 3) {"));
        assertTrue(c.contains("if (i == 1) {"));
        assertTrue(c.contains("} else {"));
    }

    @Test
    void compilesForLoops() throws IOException {
        String c = compileExample("for.kite");

        assertTrue(c.contains("for (int32_t i = 0; i < 3; i = i + 1) {"));
    }

    @Test
    void compilesPrimitiveTypes() throws IOException {
        String c = compileExample("types.kite");

        assertTrue(c.contains("#include <stdbool.h>"));
        assertTrue(c.contains("bool enabled = true;"));
        assertTrue(c.contains("uint32_t count = 3;"));
        assertTrue(c.contains("int64_t total = 10;"));
        assertTrue(c.contains("uint8_t value = 1;"));
    }

    @Test
    void compilesObjectMethodCalls() throws IOException {
        String c = compileExample("methods.kite");

        assertTrue(c.contains("kite_node n;"));
        assertTrue(c.contains("node_init(&n, 10);"));
    }

    private String compileExample(String fileName) throws IOException {
        String source = Files.readString(Path.of("examples", fileName));
        return compiler.compile(source);
    }
}
