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
        return allocator.alloc(size);
    }

    void on_error(string e) {
        console.write("error: " + e);
    }
}
```

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
