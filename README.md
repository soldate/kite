# Kite

Kite is a C-like programming language designed to be a **pretty C**:

> Simple like Java, controlled like C.

The goal is not to protect the programmer from every mistake.  
The goal is to make code clean, predictable, and explicit where it matters.

---

## Philosophy

Kite follows a few core principles:

- no garbage collector
- no hidden lifetime management
- no global magic
- no forced safety model
- happy path first
- full control when needed

> Kite may hide where memory is allocated, but it never hides when memory is freed.

---

## Basic syntax

```c
type node {
    int value;
    pointer node next;

    void init(int v) {
        value = v;
        next = 0;
    }
}
```

### Rules

- all code lives inside `type`
- names are lowercase by convention
- semicolons are required
- `if (...)`, `while (...)`, `for (...)` use C-style syntax
- function return type comes before the name

---

## Entry point

```c
type main {
    void main() {
        console.write("hello");
    }
}
```

Only one `type main` with `void main()` should exist in a program.

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

Always valid.

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

- stack if it does not escape
- heap if it escapes → `on_heap`

Force heap allocation:

```c
heap node n;
```

- `n` is still a non-null object reference
- allocation is explicit in source
- lifetime is still manual
- must be released with `delete n;`
- different from `pointer node`, which is nullable/manual pointer syntax

```c
delete obj;
```

- always manual
- forgetting = memory leak

---

## Owner

```c
type main {
    pointer on_heap(int size) {
        pointer p = allocator.alloc(size);
        heap_objects.add(p);
        return p;
    }

    void on_error(string e) {
        console.write("error: " + e);
    }
}
```

`on_heap` is the allocation hook for program objects. It is called when the compiler decides an object must live on the heap, or when the programmer forces it:

```c
heap node n;
```

Owner/runtime infrastructure must not recursively allocate through `on_heap`.
For example, if `heap_objects.add(p)` needs storage to track allocations, that storage must come from bootstrap/owner memory, not from the normal program heap hook.

Rule:

- program object heap allocation → `on_heap`
- owner/runtime bookkeeping allocation → bootstrap allocator
- `on_heap` may record returned pointers for later cleanup
- bootstrap allocation must not call `on_heap`

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
int[] numbers = array int(10);

numbers[0] = 5;
console.write(numbers.length);
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
type list {
    node head;

    void add(int v) {
        node n;

        n.value = v;
        n.next = head;

        head = n;
        // escapes → heap
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
- fields
- methods
- primitive types: `int`, `uint`, `long`, `ulong`, `float`, `double`, `byte`, `char`, `bool`, `string`, `void`
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
- local initialization syntax, such as `node n = node(10)`, lowered to `node n; n.init(10);`
- C function prototypes, so method calls do not depend on source type order
- `pointer T` types, including pointers to primitive and Kite-defined types
- Kite-defined object variables are references; local non-escaping objects currently lower to stack storage plus a pointer reference
- explicit heap object declarations with `heap T name`
- runtime `kite_on_heap(size)` helper for heap object allocation; currently backed by `malloc`
- `type main` may define `pointer on_heap(int size)` to override program-object heap allocation
- `delete expr;` lowers to explicit memory release for heap/manual pointers
- basic arrays: `T[]`, `array T(n)`, indexing with `a[i]`, and `a.length`

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
examples/heap.kite
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

It registers `.kite` as the `kite` language and provides TextMate syntax highlighting for Kite-specific keywords such as `type`, `pointer`, `copy`, `delete`, `foreach`, primitive types, strings, comments, and numbers.

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
