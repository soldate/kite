package compiler.util;

import java.io.*;
import java.nio.file.*;

import compiler.*;
import compiler.ast.core.*;
import compiler.ast.expr.*;
import compiler.ast.var_def.*;

public class Util {

	public static void debugPrintTokens(Token start, int context, Exception e) {
		System.err.println("========= Error: =========");

		// if parser error
		if (!Parser.current.kind.equals(Token.Kind.EOF)) {
			System.err.println("line: " + start.pos);
			System.err.println("-------------------------");
			// Walk some tokens back to find the context
			Token t = start;
			for (int i = 0; i < context && t.prev != null; i++) {
				t = t.prev;
			}

			// Walk some tokens front to find the context
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

	public static int getClassSize(ClassDefNode clazz) {
		int size = 8; // pointer size
		for (VarDeclNode field : clazz.fields.values()) {
			size += getFieldSize(field);
		}
		return size;
	}

	public static int getFieldOffset(FieldAccessNode fa) {
		ClassDefNode clazz = resolveClassOfFieldAccess(fa.target);
		int fieldOffset = resolveFieldOffset(clazz, fa.field);
		return fieldOffset;
	}

	public static int getFieldSize(VarDeclNode field) {
		if (field.typeClass != null) {
			return getClassSize(field.typeClass);
		} else {
			return 8;
		}
	}

	public static String loadKiteFile(String fileName) throws IOException {
		String kiteFile = fileName.trim();
		kiteFile = kiteFile.replace(".kite", "_kite");
		kiteFile = kiteFile.replaceAll("\\.", "/");
		kiteFile = kiteFile.replaceAll("_kite", ".kite");
		return new String(Files.readAllBytes(Paths.get(kiteFile)));
	}

	public static IdentNode resolveBaseIdent(FieldAccessNode fa) {
		Node current = fa;
		while (current instanceof FieldAccessNode f)
			current = f.target;

		if (current instanceof IdentNode id) return id;

		throw new RuntimeException("Unsupported nested base in FieldAccessNode");
	}

	public static ClassDefNode resolveClassOfFieldAccess(Node target) {
		if (target instanceof IdentNode idTarget) {
			VarDeclNode varDecl = idTarget.varDecl;
			return varDecl.typeClass;

		} else if (target instanceof VarDeclNode faTarget) {
			return faTarget.typeClass;

		} else if (target instanceof FieldAccessNode faTarget) {
			ClassDefNode outerClass = resolveClassOfFieldAccess(faTarget.target);
			VarDeclNode fieldDecl = outerClass.fields.get(faTarget.field);
			return fieldDecl.typeClass;
		}
		throw new RuntimeException("Unsupported FieldAccessNode target: " + target);
	}

	public static int resolveFieldOffset(ClassDefNode classNode, String fieldName) {
		int offset = 8; // starts with the pointer size
		for (VarDeclNode field : classNode.fields.values()) {
			if (field.name.equals(fieldName)) return offset;
			offset += getFieldSize(field);
		}
		throw new RuntimeException("Field not found: " + fieldName + " in class " + classNode.name);
	}

}
