package compiler;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import compiler.ast.*;

public class CodeGen {
	private final PrintWriter out;
	private final Map<String, Integer> localVars = new HashMap<>();
	private int stackOffset = 0;
	private int labelCounter = 0;

	public CodeGen(PrintWriter out) {
		this.out = out;
	}

	private void gen(Node node) {
		if (node instanceof BlockNode block) {
			for (Node stmt : block.statements) {
				gen(stmt);
			}

		} else if (node instanceof NumNode num) {
			out.printf("    mov $%d, %%rax\n", num.value);

		} else if (node instanceof IdentNode ident) {
			Integer offset = localVars.get(ident.name);
			if (offset == null) throw new RuntimeException("Undefined variable: " + ident.name);
			out.printf("    mov %d(%%rbp), %%rax\n", offset);

		} else if (node instanceof VarDeclNode var) {
			stackOffset -= 8;
			localVars.put(var.name, stackOffset);
			gen(var.value);
			out.printf("    mov %%rax, %d(%%rbp)\n", stackOffset);

		} else if (node instanceof AssignNode assign) {
			Integer offset = localVars.get(assign.name);
			if (offset == null) throw new RuntimeException("Undefined variable: " + assign.name);
			gen(assign.value);
			out.printf("    mov %%rax, %d(%%rbp)\n", offset);

		} else if (node instanceof ReturnNode ret) {
			gen(ret.expr);

		} else if (node instanceof IfNode ifn) {
			int label = labelCounter++;
			gen(ifn.cond);
			out.println("    cmp $0, %rax");
			out.printf("    je .Lend%d\n", label);
			gen(ifn.thenBranch);
			out.printf(".Lend%d:\n", label);

		} else if (node instanceof WhileNode wn) {
			int label = labelCounter++;
			out.printf(".Lbegin%d:\n", label);
			gen(wn.cond);
			out.println("    cmp $0, %rax");
			out.printf("    je .Lend%d\n", label);
			gen(wn.body);
			out.printf("    jmp .Lbegin%d\n", label);
			out.printf(".Lend%d:\n", label);

		} else if (node instanceof BinOpNode bin) {
			gen(bin.left); // avalia left
			out.println("    push %rax"); // salva left
			gen(bin.right); // avalia right
			out.println("    pop %rdi"); // recupera left em rdi

			switch (bin.op) {
			case PLUS -> out.println("    add %rdi, %rax"); // rax = right + left
			case MINUS -> {
				out.println("    sub %rax, %rdi"); // rdi = left - right
				out.println("    mov %rdi, %rax"); // rax = resultado
			}
			case MUL -> out.println("    imul %rdi, %rax");
			case DIV -> {
				out.println("    mov %rdi, %rcx"); // left
				out.println("    mov %rax, %rdi"); // right
				out.println("    mov %rcx, %rax"); // rax = left
				out.println("    cqo");
				out.println("    idiv %rdi"); // rax = left / right
			}
			default -> throw new RuntimeException("Unsupported binary operator");
			}

		} else {
			throw new RuntimeException("Unsupported node type: " + node.getClass().getSimpleName());
		}
	}

	public void emit(Node node) {
        out.println(".globl main");
        out.println("main:");
        out.println("    push %rbp");
        out.println("    mov %rsp, %rbp");

        gen(node);

        out.println("    mov %rbp, %rsp");
        out.println("    pop %rbp");
        out.println("    ret");
    }
}
