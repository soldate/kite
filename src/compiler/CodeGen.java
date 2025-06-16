package compiler;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import compiler.ast.*;
import compiler.util.MyPrintWriter;

class CodeGen {
	private final PrintWriter out;
    private final Map<String, Integer> localVars = new HashMap<>();
    private int stackOffset = 0;
    private int labelCounter = 0;

	CodeGen(PrintWriter out) {
        this.out = out;
    }

    private void gen(Node node) {
        if (node instanceof compiler.ast.BlockNode block) {
            for (Node stmt : block.statements) gen(stmt);

        } else if (node instanceof compiler.ast.NumNode num) {
            out.printf("    mov $%d, %%rax\n", num.value);

        } else if (node instanceof compiler.ast.IdentNode ident) {
            Integer offset = localVars.get(ident.name);
            if (offset == null) throw new RuntimeException("Undefined variable: " + ident.name);
            out.printf("    mov %d(%%rbp), %%rax\n", offset);

        } else if (node instanceof compiler.ast.BinOpNode bin) {
            gen(bin.right);
            out.println("    push %rax");
            gen(bin.left);
            out.println("    pop %rdi");
            switch (bin.op) {
                case PLUS -> out.println("    add %rdi, %rax");
                case MINUS -> out.println("    sub %rdi, %rax");
                case MUL -> out.println("    imul %rdi, %rax");
                case DIV -> {
                    out.println("    mov %rax, %rcx");
                    out.println("    mov %rdi, %rax");
                    out.println("    cqo");
                    out.println("    idiv %rcx");
                }
                case EQ, NEQ, LT, GT, LE, GE -> {
                    out.println("    cmp %rdi, %rax");
                    switch (bin.op) {
                        case EQ -> out.println("    sete %al");
                        case NEQ -> out.println("    setne %al");
                        case LT -> out.println("    setl %al");
                        case GT -> out.println("    setg %al");
                        case LE -> out.println("    setle %al");
                        case GE -> out.println("    setge %al");
                    }
                    out.println("    movzb %al, %rax");
                }
                default -> throw new RuntimeException("Unsupported operator");
            }

        } else if (node instanceof compiler.ast.VarDeclNode var) {
            stackOffset -= 8;
            localVars.put(var.name, stackOffset);
            gen(var.value);
            out.printf("    mov %%rax, %d(%%rbp)\n", stackOffset);

        } else if (node instanceof compiler.ast.AssignNode assign) {
            Integer offset = localVars.get(assign.name);
            if (offset == null) throw new RuntimeException("Undefined variable: " + assign.name);
            gen(assign.value);
            out.printf("    mov %%rax, %d(%%rbp)\n", offset);

        } else if (node instanceof compiler.ast.ReturnNode ret) {
            gen(ret.expr);

        } else if (node instanceof compiler.ast.IfNode ifn) {
            int label = labelCounter++;
            gen(ifn.cond);
            out.println("    cmp $0, %rax");
            out.printf("    je .Lend%d\n", label);
            gen(ifn.thenBranch);
            out.printf(".Lend%d:\n", label);

        } else if (node instanceof compiler.ast.WhileNode wn) {
            int label = labelCounter++;
            out.printf(".Lbegin%d:\n", label);
            gen(wn.cond);
            out.println("    cmp $0, %rax");
            out.printf("    je .Lend%d\n", label);
            gen(wn.body);
            out.printf("    jmp .Lbegin%d\n", label);
            out.printf(".Lend%d:\n", label);

        } else if (node instanceof compiler.ast.FuncCallNode fn) {
            String[] argRegs = {"%rdi", "%rsi", "%rdx", "%rcx", "%r8", "%r9"};
            int n = fn.args.size();
            if (n > argRegs.length) throw new RuntimeException("Too many arguments");
            for (int i = n - 1; i >= 0; i--) {
                gen(fn.args.get(i));
                out.println("    push %rax");
            }
            for (int i = 0; i < n; i++) {
                out.printf("    pop %s\n", argRegs[i]);
            }
            out.printf("    call %s\n", fn.name);

        } else if (node instanceof compiler.ast.FuncDefNode fd) {
            out.printf(".globl %s\n", fd.name);
            out.printf("%s:\n", fd.name);
            out.println("    push %rbp");
            out.println("    mov %rsp, %rbp");
            localVars.clear();
            stackOffset = 0;
            String[] argRegs = {"%rdi", "%rsi", "%rdx", "%rcx", "%r8", "%r9"};
            for (int i = 0; i < fd.params.size(); i++) {
                stackOffset -= 8;
                localVars.put(fd.params.get(i), stackOffset);
                out.printf("    mov %s, %d(%%rbp)\n", argRegs[i], stackOffset);
            }
            gen(fd.body);
            out.println("    mov %rbp, %rsp");
            out.println("    pop %rbp");
            out.println("    ret");

        } else {
            throw new RuntimeException("Unsupported node type: " + node.getClass().getSimpleName());
        }
    }

	void emit(Node node) {
        // Emit function definitions first
        if (node instanceof compiler.ast.BlockNode block) {
            for (Node stmt : block.statements) {
                if (stmt instanceof compiler.ast.FuncDefNode) gen(stmt);
            }
        }

        // Emit main entry point
        out.println(".globl main");
        out.println("main:");
        out.println("    push %rbp");
        out.println("    mov %rsp, %rbp");

        if (node instanceof compiler.ast.BlockNode block) {
            for (Node stmt : block.statements) {
                if (!(stmt instanceof compiler.ast.FuncDefNode)) gen(stmt);
            }
        } else {
            gen(node);
        }

        out.println("    mov %rbp, %rsp");
        out.println("    pop %rbp");
        out.println("    ret");
    }
}
