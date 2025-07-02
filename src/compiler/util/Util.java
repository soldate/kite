package compiler.util;

import compiler.*;
import compiler.ast.core.*;
import compiler.ast.expr.*;
import compiler.ast.var_def.*;

public class Util {

	public static void debugPrintTokens(Token start, int context, Exception e) {
		System.err.println("========= Erro: =========");

		// if parser error
		if (!Parser.current.kind.equals(Token.Kind.EOF)) {
			System.err.println("line: " + start.pos);
			System.err.println("-------------------------");
			// Anda alguns tokens para trás (se possível)
			Token t = start;
			for (int i = 0; i < context && t.prev != null; i++) {
				t = t.prev;
			}

			// Agora anda para frente e imprime o contexto
			for (int i = 0; i < context * 2 && t != null; i++) {
				String marker = (t == Parser.current) ? "  <-- current" : "";
				System.err.printf("[%s] \"%s\"%s\n", t.kind, t.text, marker);
				t = t.next;
			}
		}

		// System.err.println(e.getMessage());
		e.printStackTrace();
		System.err.println("=========================");
	}

	public static int getClassSize(ClassNode clazz) {
		int size = 8; // ponteiro interno
		for (VarDeclNode field : clazz.fields.values()) {
			size += getFieldSize(field);
		}
		return size;
	}

	public static int getFieldOffset(FieldAccessNode fa) {
		ClassNode clazz = resolveClassOfFieldAccess(fa.target);
		int fieldOffset = resolveFieldOffset(clazz, fa.field);
		return fieldOffset;
	}

	public static int getFieldSize(VarDeclNode field) {
		// Campos que são objetos (inclusive recursivos) ocupam o tamanho da classe
		if (field.typeClass != null) {
			return getClassSize(field.typeClass);
		} else {
			return 8;
		}
	}

	public static boolean isObjectType(String type) {
		return !(type.equals("int") || type.equals("bool") || type.equals("char") || type.equals("byte")
				|| type.equals("uint") || type.equals("float"));
	}

	public static IdentNode resolveBaseIdent(FieldAccessNode fa) {
		Node current = fa;
		while (current instanceof FieldAccessNode f)
			current = f.target;

		if (current instanceof IdentNode id) return id;

		throw new RuntimeException("Unsupported nested base in FieldAccessNode");
	}

	// Novo método auxiliar
	public static ClassNode resolveClassOfFieldAccess(Node target) {
		if (target instanceof IdentNode idTarget) {
			VarDeclNode varDecl = idTarget.varDecl;
			return varDecl.typeClass;

		} else if (target instanceof VarDeclNode faTarget) {
			return faTarget.typeClass;

		} else if (target instanceof FieldAccessNode faTarget) {
			ClassNode outerClass = resolveClassOfFieldAccess(faTarget.target);
			VarDeclNode fieldDecl = outerClass.fields.get(faTarget.field);
			return fieldDecl.typeClass;
		}
		throw new RuntimeException("Unsupported FieldAccessNode target: " + target);
	}

	// Retorna o offset do campo dentro da classe, respeitando o layout com ponteiro
	// no início
	public static int resolveFieldOffset(ClassNode classNode, String fieldName) {
		int offset = 8; // começa após o ponteiro
		for (VarDeclNode field : classNode.fields.values()) {
			if (field.name.equals(fieldName)) return offset;
			offset += getFieldSize(field);
		}
		throw new RuntimeException("Field not found: " + fieldName + " in class " + classNode.name);
	}

}
