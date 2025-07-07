package compiler;

import java.io.*;
import java.util.*;

import compiler.ast.core.*;
import compiler.ast.expr.*;
import compiler.ast.stmt.*;
import compiler.ast.var_def.*;
import compiler.util.*;

class CodeGen {
	private final PrintWriter out;
	private final Deque<Map<String, Integer>> scopes = new ArrayDeque<>();
	private int stackOffset = 0;
	private int labelCounter = 0;
	private String currentReturnType = null;

	public CodeGen() {
		this.out = new PrintWriter(System.out);
	}

	public CodeGen(PrintWriter out) {
		this.out = out;
	}

	private void declareVar(String name) {
		stackOffset -= 8;
		scopes.peek().put(name, stackOffset);
	}

	private void enterScope() {
		scopes.push(new LinkedHashMap<>());
	}

	private void exitScope() {
		scopes.pop();
	}

	private void genLValueAddr(Node target) {
		if (target instanceof IdentNode id) {
			int offset = lookupVar(id.varDecl.name);
			out.printf("    mov %d(%%rbp), %%rdi\n", offset);

		} else if (target instanceof FieldAccessNode fa) {
			genLValueAddr(fa.target);
			int offset = Util.getFieldOffset(fa);
			out.printf("    add $%d, %%rdi\n", offset);

		} else {
			throw new RuntimeException("Invalid lvalue");
		}
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
		out.println("    sub $256, %rsp");

		stackOffset = 0;
		enterScope();

		String[] argRegs = { "%rdi", "%rsi", "%rdx", "%rcx", "%r8", "%r9" };
		int i = 0;
		for (VarDeclNode param : fn.params.values()) {
			declareVar(param.name);
			out.printf("    mov %s, %d(%%rbp)\n", argRegs[i], lookupVar(param.name));
			i++;
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
		if (node instanceof ProgramNode prog) {
			for (ClassDefNode clazz : prog.types.values()) {
				gen(clazz);
			}
			emit(prog.main);
		} else if (node instanceof ClassDefNode clazz) {
			for (FuncDefNode method : clazz.methods.values()) {
				gen(method);
			}
		} else if (node instanceof FuncDefNode func) {
			emit(func);
		} else if (node instanceof BlockNode block) {
			for (Node stmt : block.statements) {
				gen(stmt);
			}
		} else if (node instanceof NumNode num) {
			out.printf("    mov $%d, %%rax\n", num.value);

		} else if (node instanceof BinOpNode bin) {
			gen(bin.right);
			out.println("    push %rax");
			gen(bin.left);
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
			case EQ, LT, GT, LE, GE, NEQ -> {
				out.println("    cmp %rdi, %rax");
				switch (bin.op) {
				case EQ -> out.println("    sete %al");
				case LT -> out.println("    setl %al");
				case GT -> out.println("    setg %al");
				case LE -> out.println("    setle %al");
				case GE -> out.println("    setge %al");
				case NEQ -> out.println("    setne %al");
				}
				out.println("    movzb %al, %rax");
			}
			case AND -> {
				int label = labelCounter++;
				gen(bin.left);
				out.println("    cmp $0, %rax");
				out.printf("    je .Lfalse%d\n", label);
				gen(bin.right);
				out.println("    cmp $0, %rax");
				out.printf("    je .Lfalse%d\n", label);
				out.println("    mov $1, %rax");
				out.printf("    jmp .Lend%d\n", label);
				out.printf(".Lfalse%d:\n", label);
				out.println("    mov $0, %rax");
				out.printf(".Lend%d:\n", label);
			}
			case OR -> {
				int label = labelCounter++;
				gen(bin.left);
				out.println("    cmp $0, %rax");
				out.printf("    jne .Ltrue%d\n", label);
				gen(bin.right);
				out.println("    cmp $0, %rax");
				out.printf("    jne .Ltrue%d\n", label);
				out.println("    mov $0, %rax");
				out.printf("    jmp .Lend%d\n", label);
				out.printf(".Ltrue%d:\n", label);
				out.println("    mov $1, %rax");
				out.printf(".Lend%d:\n", label);
			}
			default -> throw new RuntimeException("Unsupported operator");
			}

		} else if (node instanceof UnaryOpNode uop) {
			gen(uop.expr);
			switch (uop.op) {
			case NOT -> {
				out.println("    cmp $0, %rax");
				out.println("    sete %al");
				out.println("    movzb %al, %rax");
			}
			default -> throw new RuntimeException("Unsupported unary operator: " + uop.op);
			}

		} else if (node instanceof VarDeclNode decl) {
			ClassDefNode clazz = decl.typeClass;

			if (clazz != null) {
				int classSize = Util.getClassSize(clazz);
				stackOffset -= classSize;
				int structOffset = stackOffset;

				scopes.peek().put(decl.name, structOffset);

				out.printf("    lea %d(%%rbp), %%rax\n", structOffset);
				out.printf("    mov %%rax, %d(%%rbp)\n", structOffset);

				for (VarDeclNode field : clazz.fields.values()) {
					if (field.typeClass != null) {
						int fieldOffset = Util.resolveFieldOffset(clazz, field.name);
						out.printf("    lea %d(%%rbp), %%rax\n", structOffset + fieldOffset);
						out.printf("    mov %%rax, %d(%%rbp)\n", structOffset + fieldOffset);
					}
				}

			} else {
				declareVar(decl.name);
				// int, bool etc.
				if (decl.value != null) {
					gen(decl.value);
					out.printf("    mov %%rax, %d(%%rbp)\n", lookupVar(decl.name));
				}
			}

		} else if (node instanceof AssignNode assign) {
			gen(assign.value);

			if (assign.target instanceof IdentNode ident) {
				int offset = lookupVar(ident.varDecl.name);
				out.printf("    mov %%rax, %d(%%rbp)\n", offset);
			} else {
				genLValueAddr(assign.target);
				out.println("    mov %rax, (%rdi)");
			}

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
			int elseLabel = labelCounter++;
			int endLabel = labelCounter++;

			gen(ifn.cond);
			out.println("    cmp $0, %rax");
			out.printf("    je .Lelse%d\n", elseLabel);

			gen(ifn.thenBranch);
			out.printf("    jmp .Lend%d\n", endLabel);

			// Else or Else-If
			out.printf(".Lelse%d:\n", elseLabel);
			if (ifn.elseBranch != null) gen(ifn.elseBranch);

			out.printf(".Lend%d:\n", endLabel);

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
			for (int i = n - 1; i >= 0; i--) {
				out.printf("    pop %s\n", argRegs[i]);
			}

			out.printf("    call %s\n", fn.name);

		} else if (node instanceof IdentNode ident) {
			out.printf("    mov %d(%%rbp), %%rax\n", lookupVar(ident.varDecl.name));

		} else if (node instanceof FieldAccessNode fa) {
			genLValueAddr(fa);
			out.println("    mov (%rdi), %rax");

		} else {
			throw new RuntimeException("Unsupported node type: " + node.getClass().getSimpleName());
		}
	}

}
