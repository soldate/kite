package compiler;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import compiler.ast.*;

class CodeGen {
	private final PrintWriter out;
    private final Map<String, Integer> localVars = new HashMap<>();
    private int stackOffset = 0;
    private int labelCounter = 0;

	CodeGen(PrintWriter out) {
        this.out = out;
    }

	void emit(FuncDefNode fn) {
        out.printf(".globl %s\n", fn.name);
        out.printf("%s:\n", fn.name);
        out.println("    push %rbp");
        out.println("    mov %rsp, %rbp");

        stackOffset = 0;
        localVars.clear();

		String[] argRegs = { "%rdi", "%rsi", "%rdx", "%rcx", "%r8", "%r9" };
		for (int i = 0; i < fn.params.size(); i++) {
			stackOffset -= 8;
			localVars.put(fn.params.get(i), stackOffset);
			out.printf("    mov %s, %d(%%rbp)\n", argRegs[i], stackOffset);
		}

        List<Node> stmts = fn.body.statements;
        for (Node stmt : stmts) {
            gen(stmt);
        }

        if (stmts.isEmpty() || !(stmts.get(stmts.size() - 1) instanceof ReturnNode)) {
            out.println("    mov %rbp, %rsp");
            out.println("    pop %rbp");
            out.println("    ret");
        }
    }

    public void gen(Node node) {
        if (node instanceof compiler.ast.BlockNode block) {
            for (Node stmt : block.statements) gen(stmt);

        } else if (node instanceof compiler.ast.NumNode num) {
            out.printf("    mov $%d, %%rax\n", num.value);

        } else if (node instanceof compiler.ast.IdentNode ident) {
            Integer offset = localVars.get(ident.name);
            if (offset == null) throw new RuntimeException("Undefined variable: " + ident.name);
            out.printf("    mov %d(%%rbp), %%rax\n", offset);

        } else if (node instanceof compiler.ast.BinOpNode bin) {
			gen(bin.left);
			out.println("    push %rax");
			gen(bin.right);
			out.println("    pop %rdi");
			switch (bin.op) {
			case PLUS -> out.println("    add %rdi, %rax");
			case MINUS -> {
				out.println("    mov %rax, %rcx"); // b em rcx
				out.println("    mov %rdi, %rax"); // a em rax
				out.println("    sub %rcx, %rax"); // rax = a - b
			}
			case MUL -> out.println("    imul %rdi, %rax");
			case DIV -> {
				out.println("    mov %rax, %rcx"); // b em rcx
				out.println("    mov %rdi, %rax"); // a em rax
				out.println("    cqo");
				out.println("    idiv %rcx"); // a / b
			}
			case EQ, NEQ, LT, GT, LE, GE -> {
				out.println("    cmp %rax, %rdi"); // compara a com b
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

		} else if (node instanceof VarDeclNode decl) {
			stackOffset -= 8;
			localVars.put(decl.name, stackOffset);

			if (decl.value != null) {
				gen(decl.value);
				out.printf("    mov %%rax, %d(%%rbp)\n", stackOffset);
			}

		} else if (node instanceof AssignNode assign) {
            gen(assign.value);

			Integer offset = localVars.get(assign.target.name);
			if (offset == null) throw new RuntimeException("Undefined variable: " + assign.target.name);

            out.printf("    mov %%rax, %d(%%rbp)\n", offset);

		} else if (node instanceof compiler.ast.ReturnNode ret) {
			if (ret.expr != null) gen(ret.expr);
			out.println("    mov %rbp, %rsp");
			out.println("    pop %rbp");
			out.println("    ret");

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

		} else if (node instanceof FuncCallNode fn) {
			String[] argRegs = { "%rdi", "%rsi", "%rdx", "%rcx", "%r8", "%r9" };
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

}
