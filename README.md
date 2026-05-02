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

Kite uses a C-like syntax:

```kite
type node {
    int value;
    pointer node next;

    void init(int v) {
        value = v;
        next = 0;
    }
}
```

Rules:

- all code lives inside `type`
- names are lowercase by convention
- semicolons are required
- `if (...)`, `while (...)`, `for (...)` use C-style syntax
- function return type comes before the name

---

## Entry point

The program entry point is:

```kite
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

```text
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

## One-line summary

> Kite is a pretty C: clean syntax, Java-like references, C-like control, no GC, and explicit lifetime.
