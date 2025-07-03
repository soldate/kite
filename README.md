**Kite aims to replace C, C++ and Java.**

## 🪁 Kite Programming Language

**Kite** is a programming language influenced by three key foundations:

- The **simplicity and explicit control** of C  
- The **zero-cost abstraction** mindset of C++  
- And the **class-based, large standard library** approach of Java  

But with a twist:  
> **In Kite, the programmer decides — not the language designer.**

---

### 🧠 Philosophy

Kite was created with the belief that a language should not enforce rigid models. Instead, it offers **clarity**, **predictability**, and **explicit choices**.

Everything is class-based. All access is done through **fat pointers**. There are no static fields or methods — to access anything, an object must be instantiated.  
This enables a powerful and novel error and memory control model.

---

### ✨ Key Features

#### 🔹 Error Handling

The programmer chooses how to deal with failures:

- Manual handling via `obj.error`
- Or automatic exception throwing with `@WithExceptions`

#### 🔹 Memory Management

Kite supports flexible memory models:

- Full manual control  
- Optional garbage collector via `@WithGarbageCollector`  
- Allocation tracking with `@CallMethodWhenAlloc("myLogger")` to monitor and verify cleanup

---

### 🔧 Current Status

Kite is under active development, and already includes:

- A Java-based compiler  
- x64 Assembly code generation  
- Primitive types, user-defined classes, methods, and inline objects with internal fat pointers  
- Dozens of tests in `src/test/kite/`

---

👉 Explore the project on GitHub:  
[**https://github.com/soldate/kite**](https://github.com/soldate/kite)

Feel free to explore, test, suggest features, or contribute.  
The goal is simple: **a lean, powerful language that respects the developer.**
