package kite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
                #include <stdlib.h>

                static void console_write(const char* text) { printf("%s", text); }

                static void* bootstrap_alloc(int32_t size) { return malloc(size); }

                #define KITE_TYPE_main 1

                typedef struct kite_main {
                } kite_main;

                static void* kite_on_heap(size_t size, int32_t type) { (void)type; return bootstrap_alloc((int32_t)size); }

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

        assertTrue(c.contains("void node_init(kite_node* self, int32_t v);"));
        assertTrue(c.contains("kite_node _n_storage;"));
        assertTrue(c.contains("kite_node* n = &_n_storage;"));
        assertTrue(c.contains("node_init(n, 10);"));
        assertFalse(c.contains("= node(10)"));
    }

    @Test
    void emitsFunctionPrototypesSoTypeOrderDoesNotMatter() throws IOException {
        String c = compileExample("order.kite");

        assertTrue(c.indexOf("void node_init(kite_node* self, int32_t v);")
                < c.indexOf("int main(void)"));
        assertTrue(c.indexOf("int main(void)") < c.indexOf("void node_init(kite_node* self, int32_t v) {"));
    }

    @Test
    void compilesPointerTypes() throws IOException {
        String c = compileExample("pointers.kite");

        assertTrue(c.contains("struct kite_node* next;"));
        assertTrue(c.contains("int32_t* p = 0;"));
        assertTrue(c.contains("self->next = 0;"));
        assertTrue(c.contains("p = p + 1;"));
    }

    @Test
    void compilesObjectsAsReferences() throws IOException {
        String c = compileExample("references.kite");

        assertTrue(c.contains("kite_node* a = kite_on_heap(sizeof(kite_node), KITE_TYPE_node);"));
        assertTrue(c.contains("node_init(a, 10);"));
        assertTrue(c.contains("kite_node* b = a;"));
        assertTrue(c.contains("b->value = 20;"));
        assertTrue(c.contains("free(a);"));
    }

    @Test
    void compilesDefaultHeapObjects() throws IOException {
        String c = compileExample("default_heap.kite");

        assertTrue(c.contains("static void* kite_on_heap(size_t size, int32_t type) { return main_on_heap(&kite_owner, (int32_t)size, type); }"));
        assertTrue(c.contains("void* main_on_heap(kite_main* self, int32_t size, int32_t type) {"));
        assertTrue(c.contains("kite_pointer_list heap_objects;"));
        assertFalse(c.contains("kite_list heap_objects;"));
        assertTrue(c.contains("if (type == KITE_TYPE_node) {"));
        assertTrue(c.contains("pointer_list_add(&self->heap_objects, p);"));
        assertTrue(c.contains("kite_node* a = kite_on_heap(sizeof(kite_node), KITE_TYPE_node);"));
        assertTrue(c.contains("node_init(a, 10);"));
        assertTrue(c.contains("kite_node* b = kite_on_heap(sizeof(kite_node), KITE_TYPE_node);"));
        assertTrue(c.contains("main_clean_heap(&kite_owner);"));
        assertTrue(c.contains("pointer_list_delete_all(&self->heap_objects);"));
        assertFalse(c.contains("_a_storage"));
        assertFalse(c.contains("_b_storage"));
    }

    @Test
    void compilesMainOnHeapHook() throws IOException {
        String c = compileExample("owner.kite");

        assertTrue(c.contains("#define KITE_TYPE_node"));
        assertTrue(c.contains("#define KITE_TYPE_box"));
        assertTrue(c.contains("void* main_on_heap(kite_main* self, int32_t size, int32_t type);"));
        assertTrue(c.contains("static kite_main kite_owner;"));
        assertTrue(c.contains("static void* kite_on_heap(size_t size, int32_t type) { return main_on_heap(&kite_owner, (int32_t)size, type); }"));
        assertTrue(c.contains("void* main_on_heap(kite_main* self, int32_t size, int32_t type) {"));
        assertTrue(c.contains("void* p = bootstrap_alloc(size);"));
        assertTrue(c.contains("if (type == KITE_TYPE_node) {"));
        assertFalse(c.contains("allocator.alloc"));
        assertTrue(c.contains("pointer_list_add(&self->node_objects, p);"));
        assertTrue(c.contains("pointer_list_add(&self->box_objects, p);"));
        assertTrue(c.contains("void main_on_delete(kite_main* self, void* p, int32_t type);"));
        assertTrue(c.contains("void main_on_delete(kite_main* self, void* p, int32_t type) {"));
        assertTrue(c.contains("pointer_list_remove(&self->node_objects, p);"));
        assertTrue(c.contains("pointer_list_remove(&self->box_objects, p);"));
        assertTrue(c.contains("free(p);"));
        assertTrue(c.contains("void main_clean_heap(kite_main* self) {"));
        assertTrue(c.contains("pointer_list_delete_all(&self->node_objects);"));
        assertTrue(c.contains("pointer_list_delete_all(&self->box_objects);"));
        assertTrue(c.contains("kite_node* first = kite_on_heap(sizeof(kite_node), KITE_TYPE_node);"));
        assertTrue(c.contains("kite_node* second = kite_on_heap(sizeof(kite_node), KITE_TYPE_node);"));
        assertTrue(c.contains("kite_box* payload = kite_on_heap(sizeof(kite_box), KITE_TYPE_box);"));
        assertTrue(c.contains("main_on_delete(&kite_owner, second, KITE_TYPE_node);"));
        assertTrue(c.contains("main_clean_heap(&kite_owner);"));
    }

    @Test
    void compilesArrays() throws IOException {
        String c = compileExample("arrays.kite");

        assertTrue(c.contains("typedef struct kite_array_int {"));
        assertTrue(c.contains("int32_t length;"));
        assertTrue(c.contains("int32_t* data;"));
        assertTrue(c.contains("kite_array_int _numbers_storage;"));
        assertTrue(c.contains("kite_array_int* numbers = &_numbers_storage;"));
        assertTrue(c.contains("int32_t _numbers_storage_data[3];"));
        assertTrue(c.contains("numbers->length = 3;"));
        assertTrue(c.contains("numbers->data = _numbers_storage_data;"));
        assertTrue(c.contains("numbers->data[0] = 5;"));
        assertTrue(c.contains("numbers->data[1] = 7;"));
        assertTrue(c.contains("kite_array_int _fixed_storage;"));
        assertTrue(c.contains("int32_t _fixed_storage_data[3];"));
        assertTrue(c.contains("fixed->data = _fixed_storage_data;"));
        assertTrue(c.contains("if (numbers->length == 3) {"));
    }

    @Test
    void rejectsDeleteOfStackObjects() {
        KiteException error = assertThrows(KiteException.class, () -> compiler.compile("""
                type node {
                    int value;
                }

                type main {
                    void main() {
                        stack node n;
                        delete n;
                    }
                }
                """));

        assertEquals("Cannot delete stack object 'n'", error.getMessage());
    }

    @Test
    void rejectsListOutsideOwnerFieldsForNow() {
        KiteException error = assertThrows(KiteException.class, () -> compiler.compile("""
                type main {
                    void main() {
                        list values;
                    }
                }
                """));

        assertEquals("list is currently supported only as a type main owner field", error.getMessage());
    }

    @Test
    void rejectsUnsupportedAllocatorMethods() {
        KiteException error = assertThrows(KiteException.class, () -> compiler.compile("""
                type main {
                    void main() {
                        pointer p = allocator.reset(1);
                    }
                }
                """));

        assertEquals("Unsupported allocator method 'reset'", error.getMessage());
    }

    @Test
    void rejectsAllocatorAllocWithWrongArity() {
        KiteException error = assertThrows(KiteException.class, () -> compiler.compile("""
                type main {
                    void main() {
                        pointer p = allocator.alloc();
                    }
                }
                """));

        assertEquals("allocator.alloc expects 1 argument", error.getMessage());
    }

    @Test
    void rejectsAllocatorAllocWithNonIntegerSize() {
        KiteException error = assertThrows(KiteException.class, () -> compiler.compile("""
                type main {
                    void main() {
                        pointer p = allocator.alloc("large");
                    }
                }
                """));

        assertEquals("allocator.alloc size must be an integer", error.getMessage());
    }

    @Test
    void rejectsAllocatorFreeWithNonPointer() {
        KiteException error = assertThrows(KiteException.class, () -> compiler.compile("""
                type main {
                    void main() {
                        allocator.free(1);
                    }
                }
                """));

        assertEquals("allocator.free expects a pointer", error.getMessage());
    }

    @Test
    void rejectsAllocatorAllocOutsideMain() {
        KiteException error = assertThrows(KiteException.class, () -> compiler.compile("""
                type helper {
                    pointer alloc(int size) {
                        return allocator.alloc(size);
                    }
                }

                type main {
                    void main() {
                    }
                }
                """));

        assertEquals("allocator.alloc is currently supported only inside type main", error.getMessage());
    }

    @Test
    void rejectsOnHeapWithInvalidReturnType() {
        KiteException error = assertThrows(KiteException.class, () -> compiler.compile("""
                type main {
                    int on_heap(int size) {
                        return 0;
                    }

                    void main() {
                    }
                }
                """));

        assertEquals("on_heap must return pointer", error.getMessage());
    }

    @Test
    void rejectsOnHeapWithInvalidParameterCount() {
        KiteException error = assertThrows(KiteException.class, () -> compiler.compile("""
                type main {
                    pointer on_heap() {
                        return allocator.alloc(1);
                    }

                    void main() {
                    }
                }
                """));

        assertEquals("on_heap expects 1 or 2 parameters", error.getMessage());
    }

    @Test
    void rejectsOnHeapWithInvalidTypeParameter() {
        KiteException error = assertThrows(KiteException.class, () -> compiler.compile("""
                type main {
                    pointer on_heap(int size, string type) {
                        return allocator.alloc(size);
                    }

                    void main() {
                    }
                }
                """));

        assertEquals("on_heap second parameter must be int type", error.getMessage());
    }

    @Test
    void rejectsOnDeleteWithInvalidReturnType() {
        KiteException error = assertThrows(KiteException.class, () -> compiler.compile("""
                type main {
                    pointer on_delete(pointer p, int type) {
                        return p;
                    }

                    void main() {
                    }
                }
                """));

        assertEquals("on_delete must return void", error.getMessage());
    }

    @Test
    void rejectsOnDeleteWithInvalidParameters() {
        KiteException error = assertThrows(KiteException.class, () -> compiler.compile("""
                type main {
                    void on_delete(int p, int type) {
                    }

                    void main() {
                    }
                }
                """));

        assertEquals("on_delete first parameter must be pointer", error.getMessage());
    }

    @Test
    void rejectsUnsupportedOwnerListMethods() {
        KiteException error = assertThrows(KiteException.class, () -> compiler.compile("""
                type main {
                    list heap_objects;

                    void main() {
                        heap_objects.clear();
                    }
                }
                """));

        assertEquals("Unsupported owner list method 'clear'", error.getMessage());
    }

    @Test
    void rejectsOwnerListAddWithWrongArity() {
        KiteException error = assertThrows(KiteException.class, () -> compiler.compile("""
                type main {
                    list heap_objects;

                    void main() {
                        heap_objects.add();
                    }
                }
                """));

        assertEquals("owner list add expects 1 argument", error.getMessage());
    }

    @Test
    void rejectsOwnerListAddWithNonPointer() {
        KiteException error = assertThrows(KiteException.class, () -> compiler.compile("""
                type main {
                    list heap_objects;

                    void main() {
                        heap_objects.add(1);
                    }
                }
                """));

        assertEquals("owner list add expects a pointer", error.getMessage());
    }

    @Test
    void rejectsObsoleteArrayCreationSyntax() {
        KiteException error = assertThrows(KiteException.class, () -> compiler.compile("""
                type main {
                    void main() {
                        int[] values = array int(3);
                    }
                }
                """));

        assertEquals("array T(n) syntax is obsolete; use T[n] name or T[] name = [items]",
                error.getMessage());
    }

    @Test
    void rejectsArrayLiteralLengthMismatch() {
        KiteException error = assertThrows(KiteException.class, () -> compiler.compile("""
                type main {
                    void main() {
                        int[2] values = [1, 2, 3];
                    }
                }
                """));

        assertEquals("Array literal length does not match declared size", error.getMessage());
    }

    @Test
    void rejectsObjectArraysWithoutExplicitStack() {
        KiteException error = assertThrows(KiteException.class, () -> compiler.compile("""
                type node {
                    int value;
                }

                type main {
                    void main() {
                        node[3] nodes;
                    }
                }
                """));

        assertEquals("Object arrays must be explicit stack arrays", error.getMessage());
    }

    @Test
    void compilesExplicitStackObjectArrays() {
        String c = compiler.compile("""
                type node {
                    int value;
                }

                type main {
                    void main() {
                        stack node[3] nodes;
                    }
                }
                """);

        assertTrue(c.contains("typedef struct kite_array_node {"));
        assertTrue(c.contains("kite_node** data;"));
        assertTrue(c.contains("kite_node _nodes_storage_objects[3];"));
        assertTrue(c.contains("kite_node* _nodes_storage_data[3];"));
        assertTrue(c.contains("nodes->data[0] = &_nodes_storage_objects[0];"));
        assertTrue(c.contains("nodes->data[2] = &_nodes_storage_objects[2];"));
    }

    private String compileExample(String fileName) throws IOException {
        String source = Files.readString(Path.of("examples", fileName));
        return compiler.compile(source);
    }
}
