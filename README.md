# Kite

Kite is a C-like programming language designed to be a **pretty C**:

> Simple like Java, controlled like C.

The goal is not to protect the programmer from every mistake.  
The goal is to make code clean, predictable, and explicit where it matters.

---

## Philosophy

Kite follows a few core principles:

- no garbage collector
- no hidden allocation or lifetime management
- no global magic
- no inversion of control
- no forced safety model
- happy path first
- full control when needed

Kite treats the running program as the programmer's responsibility.
The execution line should belong to the code that is actually running, not to a framework, runtime, garbage collector, or hidden scheduler.
Libraries may help, but they must not take ownership of the program.

This means important effects should pass through explicit program-owned hooks.
Allocating memory, releasing memory, opening files, keeping handles, scheduling work, or registering long-lived resources should be visible to the owner of the program.
The owner does not need to implement everything manually, but it must be aware of what is happening and decide which policy is being used.

In Kite, responsibility is part of the design:

- the programmer owns the execution flow
- the owner owns resource policy
- libraries request resources instead of silently taking control
- cleanup is explicit
- hidden global lifecycle management is avoided

---

## Basic syntax

```c
type node {
    int value;
    pointer node next; // manual pointer, can be 0 (null)

    void init(int v) {
        value = v;
        next = 0;
    }
}
```

### Rules

- all code lives inside `type` or `special type`
- names are lowercase by convention
- semicolons are required
- `if (...)`, `while (...)`, `for (...)` use C-style syntax
- function return type comes before the name

---

## Entry point

```c
special type main {
    void main() {
        console.write("hello");
    }
}
```

Only one `special type main` with `void main()` should exist in a program.

---

## Types

Basic types have fixed sizes:

```
int     32-bit signed integer
uint    32-bit unsigned integer
long    64-bit signed integer
ulong   64-bit unsigned integer
float   32-bit floating point
double  64-bit floating point
byte    8-bit value
char    character
bool    boolean
string  immutable string
```

---

## Objects are references

```c
node a;
node b = a;

b.value = 10;
```

Both `a` and `b` refer to the same object.

To duplicate:

```c
node b = copy a;
```

---

## Non-null by default

```c
node n;
```

Always valid. Object variables are non-null references.

Nullable/manual:

```c
pointer node p;
```

---

## Pointers

```c
pointer int p;
p = p + 1;
```

- can be `0`
- supports arithmetic
- full control

---

## Memory model

```c
node n;
```

- heap by default
- allocation goes through `on_heap`
- lifetime is manual
- must be released explicitly, usually by the owner that recorded the allocation

Force stack allocation:

```c
stack node n;
```

- `n` is still a non-null object reference
- storage is local to the current scope
- must not be released with `delete`
- different from `pointer node`, which is nullable/manual pointer syntax

```c
delete obj;
```

- always manual
- calls `on_delete(pointer p, int type)` when the owner defines it
- falls back to direct release when no owner delete hook exists
- forgetting = memory leak
- deleting twice = bug

---

## Owner

```c
special type memory {
    list heap_objects;

    pointer on_heap(int size, int type) {
        pointer p = allocator.alloc(size);

        if (type == node.id) {
            heap_objects.add(p);
        }

        return p;
    }

    void on_delete(pointer p, int type) {
        if (type == node.id) {
            heap_objects.remove(p);
        }

        allocator.free(p);
    }

    void clean_heap() {
        heap_objects.delete_all();
    }
}

special type main {
    void main() {
        node a = node(10);
        node b = node(20);

        delete b;
        memory.clean_heap();
    }
}
```

`special type memory` is the required singleton for heap policy. `on_heap` is the allocation hook for heap-allocated program objects. It is called for normal object declarations, which allocate on the heap by default:

```c
node n;
```

Owner/runtime infrastructure must not recursively allocate through `on_heap`.
For example, if `heap_objects.add(p)` needs storage to track allocations, that storage must come from bootstrap/owner memory, not from the normal program heap hook.
For now, the compiler specializes owner `list` fields into a bootstrap pointer-list implementation internally.
`allocator.alloc` allocates owner/runtime memory without going through `on_heap`. `allocator.free` releases that memory without recursively calling `on_delete`. The current C backend lowers these to `malloc`/`free` helpers.

Fields declared in `special type memory` are owner fields. If an owner field uses a `type` that performs internal allocations, the compiler must generate an owner/bootstrap variant of that type so its internal storage does not call `on_heap`.
`special type main` should stay focused on program flow and call memory singleton methods explicitly, such as `memory.clean_heap()`.

Rule:

- program object heap allocation → `on_heap`
- owner/runtime bookkeeping allocation → bootstrap allocator
- `special type memory` fields are owner/bootstrap infrastructure
- types used by owner fields must be adapted by the compiler when they allocate internally
- owner `list` currently supports `add(pointer)`, `remove(pointer)`, and `delete_all()`
- `allocator.alloc(size)` and `allocator.free(pointer)` are currently supported only inside `special type memory`; `size` must be an integer
- `on_heap` may record returned pointers for later cleanup
- `on_delete` should remove manually deleted pointers from owner tracking before releasing them
- bootstrap allocation must not call `on_heap`

`on_heap` may receive type metadata so the owner can route allocations by object type:

```c
pointer on_heap(int size, int type) {
    pointer p = allocator.alloc(size);

    if (type == myclass.id) {
        heap_objects_1.add(p);
    } else {
        heap_objects_2.add(p);
    }

    return p;
}
```

The compiler generates stable type ids for Kite-defined types and exposes them as `mytype.id`. This keeps `pointer` raw while still giving the owner enough information to organize heap allocations. The older `on_heap(int size)` form is still accepted when type metadata is not needed.

---

## Errors

```c
throw "file not found";
```

→ goes to `on_error`

---

## Strings

```c
string s = "hello";
string t = s + " world";
```

- immutable

Mutable:

```c
string_builder b;
byte[] buffer;
```

---

## Arrays

```c
int[] values = [1, 2, 3];
int[3] numbers;
stack int[3] local_numbers;

numbers[0] = 5;
console.write(numbers.length);
```

Arrays follow the same storage rule as objects:

- without `stack`, array storage is heap storage and must pass through `special type memory`
- with `stack`, array storage is local to the current scope
- the length is known by the compiler for fixed arrays and literals, and is available with `.length`

```c
int[3] values;        // heap array
stack int[3] values;  // stack array
```

Object arrays use the same rule:

```c
node[3] nodes;        // heap array storage
stack node[3] nodes;  // stack array storage
```

The old provisional form is not part of the intended language:

```c
int[] numbers = array int(3); // obsolete
```

---

## Loops

```c
for (int i = 0; i < numbers.length; i = i + 1) {
    console.write(numbers[i]);
}

foreach (n in numbers) {
    console.write(n);
}
```

---

## Multiple return values

```c
string, int read(string path) {
    return text, size;
}
```

```c
text, size = read("file.txt");
```

Ignore values:

```c
text, _ = read("file.txt");
```

---

## Initialization

```c
node n = node(10);
```

---

## Layout

```
type        → C-like layout
packed type → no padding
soa type    → struct-of-arrays
```

---

## Composition

```c
type player {
    using vec2 position;
}
```

```c
p.x = 10; // instead of p.position.x
```

---

## No anonymous functions

Use named methods.

---

## Modules

```c
import kite.io.file;
```

Filesystem:

```
kite/
  io/
  math/
```

---

## C interop

```c
extern "c" {
    int printf(pointer byte fmt);
}
```

---

## Example

```c
type node_list {
    node head;

    void add(int v) {
        node n;

        n.value = v;
        n.next = head;

        head = n;
        // node objects are heap references by default
    }
}
```

---

## One-line summary

> Kite is a pretty C: clean syntax, Java-like references, C-like control, no GC, explicit lifetime.

---

## Current compiler work

Kite currently has an early compiler/transpiler named `kitec`.

For now, `kitec` is written in Java and transpiles Kite source code to C. C is the first backend because it lets us validate the language quickly while still staying close to Kite's memory and control model.

Current implementation:

- Java 17 project managed with Maven
- hand-written lexer
- hand-written recursive-descent parser
- AST model using Java records and sealed interfaces
- C code generator
- VS Code debug configuration
- local VS Code syntax highlighting extension for `.kite`

Supported language subset:

- `type`
- `special type main` and `special type memory` for program singletons
- fields
- methods
- primitive types: `int`, `uint`, `long`, `ulong`, `float`, `double`, `byte`, `char`, `bool`, `string`, `void`
- builtin `list` type, currently supported for owner heap tracking fields
- boolean literals: `true`, `false`
- local variable declarations
- assignment
- arithmetic and comparison expressions
- function-style calls
- `console.write(...)`
- `return`
- `if` / `else`
- `while`
- `for`
- implicit `self` access for fields inside methods
- object method calls for local variables, such as `n.init(10)`
- local initialization syntax, such as `node n = node(10)`, lowered to allocation plus `n.init(10)`
- C function prototypes, so method calls do not depend on source type order
- `pointer T` types, including pointers to primitive and Kite-defined types
- Kite-defined object variables are references and allocate on heap by default
- explicit stack object declarations with `stack T name`
- runtime `kite_on_heap(size, type)` helper for heap object allocation; currently backed by `malloc`
- `special type memory` may define `pointer on_heap(int size)` or `pointer on_heap(int size, int type)` to override program-object heap allocation
- `special type memory` may define `void on_delete(pointer p, int type)`; `delete expr;` calls it when present, otherwise releases directly
- `special type main` may call memory singleton methods, such as `memory.clean_heap()`
- deleting a known `stack` object is rejected by the compiler
- array syntax: `T[] name = [items]` and `T[n] name`, with indexing via `a[i]` and length via `a.length`

Examples live in `examples/`:

```text
examples/hello.kite
examples/node.kite
examples/control.kite
examples/for.kite
examples/types.kite
examples/methods.kite
examples/order.kite
examples/pointers.kite
examples/references.kite
examples/arrays.kite
examples/default_heap.kite
examples/owner.kite
```

Build the compiler:

```bash
mvn compile
```

The repository includes `.mvn/settings.xml` and `.mvn/maven.config` so Maven uses Maven Central for project dependencies even if the machine has a private global Maven mirror configured.

or:

```bash
./build.sh
```

Transpile a Kite file to C:

```bash
java -cp target/classes kite.Main examples/hello.kite > build/hello.c
```

Compile and run the generated C:

```bash
gcc build/hello.c -o build/hello
./build/hello
```

### VS Code

The repository includes VS Code configuration in `.vscode/`.

Run and Debug configurations:

- `Debug kitec: hello.kite`
- `Debug kitec: node.kite`

These run `mvn compile` first, then launch `kite.Main` with the selected example file. Breakpoints work in the Java compiler code.

### Syntax highlighting

Kite has a small local VS Code extension in:

```text
tools/vscode-kite/
```

It registers `.kite` as the `kite` language and provides TextMate syntax highlighting for Kite-specific keywords such as `special`, `type`, `pointer`, `copy`, `delete`, `foreach`, builtin types such as `list`, primitive types, strings, comments, and numbers.

Install or reinstall it with:

```bash
tools/vscode-kite/install.sh
```

If VS Code does not refresh the highlighting immediately, run `Developer: Reload Window`.

### Dependencies

Required:

- Java 17
- Maven
- GCC or Clang to compile generated C

Recommended for VS Code:

- Extension Pack for Java
- C/C++ extension
- the local Kite syntax extension from `tools/vscode-kite`

Run tests:

```bash
mvn test
```

The test suite includes compiler tests and binary integration tests that transpile selected examples to C, compile them with `gcc`, run the resulting binaries, and check stdout.
