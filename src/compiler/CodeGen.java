package compiler;

import java.io.PrintWriter;
import java.util.*;
import compiler.ast.*;

class CodeGen {
	private final PrintWriter out;
	private final Deque<Map<String, Integer>> scopes = new ArrayDeque<>();
    private int stackOffset = 0;
    private int labelCounter = 0;
	private String currentReturnType = null;

	CodeGen(PrintWriter out) {
        this.out = out;
    }

	private int allocTemp() {
		stackOffset -= 8;
		return stackOffset;
	}

	private void declareVar(String name) {
        stackOffset -= 8;
        scopes.peek().put(name, stackOffset);
    }

	private void enterScope() {
        scopes.push(new HashMap<>());
    }

	private void exitScope() {
        scopes.pop();
    }

	private Integer lookupVar(String name) {
        for (Map<String, Integer> scope : scopes) {
            if (scope.containsKey(name)) return scope.get(name);
        }
        throw new RuntimeException("Undefined variable: " + name);
    }

	void emit(FuncDefNode fn) {
		this.currentReturnType = fn.returnType;

        out.printf(".globl %s\n", fn.name);
        out.printf("%s:\n", fn.name);
        out.println("    push %rbp");
        out.println("    mov %rsp, %rbp");
		out.println("    sub $16, %rsp");

        stackOffset = 0;
		enterScope();

		String[] argRegs = { "%rdi", "%rsi", "%rdx", "%rcx", "%r8", "%r9" };
		for (int i = 0; i < fn.params.size(); i++) {
			declareVar(fn.params.get(i));
			out.printf("    mov %s, %d(%%rbp)\n", argRegs[i], lookupVar(fn.params.get(i)));
		}

		List<Node> stmts = fn.body.statements;
		for (Node stmt : stmts)
			gen(stmt);

		if (stmts.isEmpty() || !(stmts.get(stmts.size() - 1) instanceof ReturnNode)) {
			out.println("    mov %rbp, %rsp");
			out.println("    pop %rbp");
			out.println("    ret");
        }

		exitScope();
	}

	public void gen(Node node) {
		if (node instanceof BlockNode block) {
			enterScope();
			for (Node stmt : block.statements)
				gen(stmt);
			exitScope();

		} else if (node instanceof NumNode num) {
			out.printf("    mov $%d, %%rax\n", num.value);

		} else if (node instanceof IdentNode ident) {
			out.printf("    mov %d(%%rbp), %%rax\n", lookupVar(ident.name));

		} else if (node instanceof compiler.ast.BinOpNode bin) {
			gen(bin.left);
			out.println("    push %rax");
			gen(bin.right);
			out.println("    pop %rdi");

			switch (bin.op) {
			case PLUS -> out.println("    add %rdi, %rax");
			case MINUS -> out.println("    sub %rdi, %rax");
			case MUL -> out.println("    imul %rdi, %rax");
			case DIV -> {
				out.println("    mov %rax, %rcx"); // b → rcx
				out.println("    mov %rdi, %rax"); // a → rax
				out.println("    cqo");
				out.println("    idiv %rcx"); // a / b
			}
			case EQ, NEQ, LT, GT, LE, GE -> {
				out.println("    cmp %rax, %rdi");
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
			declareVar(decl.name);
			if (decl.value != null) {
				gen(decl.value);
				out.printf("    mov %%rax, %d(%%rbp)\n", lookupVar(decl.name));
			}

		} else if (node instanceof AssignNode assign) {
            gen(assign.value);
			out.printf("    mov %%rax, %d(%%rbp)\n", lookupVar(assign.target.name));

		} else if (node instanceof ReturnNode ret) {
			if (ret.expr != null && currentReturnType.equals("void"))
				throw new RuntimeException("Cannot return a value from a void function");
			if (ret.expr == null && !currentReturnType.equals("void"))
				throw new RuntimeException("Must return a value from a non-void function");
			if (ret.expr != null) gen(ret.expr);
			out.println("    mov %rbp, %rsp");
			out.println("    pop %rbp");
			out.println("    ret");

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

		} else if (node instanceof FuncCallNode fn) {
			String[] argRegs = { "%rdi", "%rsi", "%rdx", "%rcx", "%r8", "%r9" };
			int n = fn.args.size();
			if (n > argRegs.length) throw new RuntimeException("Too many arguments");

			for (int i = 0; i < n; i++) {
				gen(fn.args.get(i));
				out.println("    push %rax");
			}
			for (int i = 0; i < n; i++) {
				out.printf("    pop %s\n", argRegs[i]);
			}

			out.printf("    call %s\n", fn.name);

		} else if (node instanceof FuncDefNode fd) {
			emit(fd);

        } else {
			throw new RuntimeException("Unsupported node type: " + node.getClass().getSimpleName());
        }
    }
}
