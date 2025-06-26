package compiler;

import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import compiler.ast.AssignNode;
import compiler.ast.BinOpNode;
import compiler.ast.BlockNode;
import compiler.ast.FieldAccessNode;
import compiler.ast.FuncCallNode;
import compiler.ast.FuncDefNode;
import compiler.ast.IdentNode;
import compiler.ast.IfNode;
import compiler.ast.Node;
import compiler.ast.NumNode;
import compiler.ast.ProgramNode;
import compiler.ast.ReturnNode;
import compiler.ast.ClassDefNode;
import compiler.ast.VarDeclNode;
import compiler.ast.WhileNode;

class CodeGen {
	private final PrintWriter out;
	private final Deque<Map<String, Integer>> scopes = new ArrayDeque<>();
    private int stackOffset = 0;
    private int labelCounter = 0;
	private String currentReturnType = null;

	// Construtor
	public CodeGen() {
		this.out = new PrintWriter(System.out);
	}

	// Construtor com PrintWriter
	public

	CodeGen(PrintWriter out) {
        this.out = out;
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
		out.println("    sub $24, %rsp");

		stackOffset = 0;
		enterScope();

		String[] argRegs = { "%rdi", "%rsi", "%rdx", "%rcx", "%r8", "%r9" };
		for (int i = 0; i < fn.params.size(); i++) {
			declareVar(fn.params.get(i).name);
			out.printf("    mov %s, %d(%%rbp)\n", argRegs[i], lookupVar(fn.params.get(i).name));
		}

		List<Node> stmts = fn.body.getStatements();
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
            for (ClassDefNode clazz : prog.classes) {
                gen(clazz);				
            }
			emit(prog.globalMain);
        } else if (node instanceof ClassDefNode clazz) {
            for (FuncDefNode method : clazz.methods) {
                gen(method);
            }
        } else if (node instanceof FuncDefNode func) {
            emit(func);						
        } else if (node instanceof BlockNode block) {
            for (Node stmt : block.getStatements()) {
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
				case EQ, LT, GT, LE, GE -> {
					out.println("    cmp %rdi, %rax");
					switch (bin.op) {
					case EQ -> out.println("    sete %al");					
					case LT -> out.println("    setl %al");
					case GT -> out.println("    setg %al");
					case LE -> out.println("    setle %al");
					case GE -> out.println("    setge %al");
					}
					out.println("    movzb %al, %rax");
				}
				case NEQ -> {
					out.println("    cmp $0, %rdi");
					out.println("    sete %al");					
				}			
				case NOT -> {
					gen(bin.left); // NOT usa só um lado
					out.println("    cmp $0, %rax");
					out.println("    sete %al");
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

		} else if (node instanceof VarDeclNode decl) {
			declareVar(decl.name);
			if (decl.value != null) {
				gen(decl.value);
				out.printf("    mov %%rax, %d(%%rbp)\n", lookupVar(decl.name));
			}

		} else if (node instanceof AssignNode assign) {
			gen(assign.value); // resultado em %rax

			if (assign.target instanceof IdentNode ident) {
				int offset = lookupVar(ident.name);
				out.printf("    mov %%rax, %d(%%rbp)\n", offset);  // valor direto na stack
			} else {
				genLValueAddr(assign.target);                      // pega endereço em %rdi
				out.println("    mov %rax, (%rdi)");               // para campos de struct
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
		
			// Then branch
			gen(ifn.thenBranch);
			out.printf("    jmp .Lend%d\n", endLabel);
		
			// Else or Else-If
			out.printf(".Lelse%d:\n", elseLabel);
			if (ifn.elseBranch != null)
				gen(ifn.elseBranch);
		
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
			if (isReference(ident)) out.printf("    lea %d(%%rbp), %%rax\n", lookupVar(ident.name));
			else out.printf("    mov %d(%%rbp), %%rax\n", lookupVar(ident.name));						

		} else if (node instanceof FieldAccessNode fa) {
			genLValueAddr(fa); // coloca o endereço do campo em %rdi
			out.println("    mov (%rdi), %rax"); // carrega o valor para %rax
							
        } else {
			throw new RuntimeException("Unsupported node type: " + node.getClass().getSimpleName());
        }
    }

	private boolean isReference(IdentNode id) {
		boolean isClass = false;
		VarDeclNode varDecl = id.currentBlock.localsMap.get(id.name);
		ClassDefNode clazz = null;
		if (varDecl != null) clazz = Parser.prog.types.get(varDecl.type);

		isClass = clazz != null?true:false;
		return isClass;
	}

	private void genLValueAddr(Node target) {
		if (target instanceof IdentNode id) {
			int offset = lookupVar(id.name);
			if (isReference(id)) out.printf("    lea %d(%%rbp), %%rdi\n", offset);
			else out.printf("    mov %d(%%rbp), %%rdi\n", offset);				

		} else if (target instanceof FieldAccessNode fa) {
			genLValueAddr(fa.target);
			int offset = getFieldOffset(fa); 
			out.printf("    add $%d, %%rdi\n", offset);
		} else {
			throw new RuntimeException("Invalid lvalue");
		}
	}
	
	private int getFieldOffset(FieldAccessNode fa) {
		ClassDefNode classNode = resolveClassOfFieldAccess(fa.target);
		return resolveFieldOffset(classNode, fa.field);
	}
	
	// Novo método auxiliar
	private ClassDefNode resolveClassOfFieldAccess(Node target) {
		if (target instanceof IdentNode id && id.name.equals("this")) {
			FuncDefNode func = id.currentBlock.parentFunc;
			ClassDefNode classNode = func.parentClass;
			return classNode;

		}else if (target instanceof IdentNode idTarget) {
			VarDeclNode varDecl = idTarget.currentBlock.localsMap.get(idTarget.name);
			return Parser.prog.types.get(varDecl.type);

		} else if (target instanceof FieldAccessNode faTarget) {
			ClassDefNode outerClass = resolveClassOfFieldAccess(faTarget.target);
			VarDeclNode fieldDecl = outerClass.fields.stream()
				.filter(f -> f.name.equals(faTarget.field))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Field not found: " + faTarget.field));
			return Parser.prog.types.get(fieldDecl.type);
		}
		throw new RuntimeException("Unsupported FieldAccessNode target: " + target);
	}	
	
	private int resolveFieldOffset(ClassDefNode classNode, String fieldName) {
		List<VarDeclNode> fields = classNode.fields;
		for (int i = 0; i < fields.size(); i++) {
			if (fields.get(i).name.equals(fieldName)) {
				return i * 8;
			}
		}
		throw new RuntimeException("Field not found: " + fieldName + " in class " + classNode.name);
	}
	
}
