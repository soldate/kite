// Mini compiler for Kite - Stage 5: block, return, if, while, and gcc+run
package compiler;

import java.io.*;
import compiler.ast.Node;
import compiler.util.MyPrintWriter;


public class Main {
    public static void main(String[] args) throws Exception {
		String input = args.length > 0 ? args[0] : "int sub(int a, int b) { return a - b; } return sub(1, 2);";
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Node ast = parser.parse();

		try (PrintWriter out = new MyPrintWriter("out.s")) {
			new CodeGen(out).emit(ast);
        }

        Process gcc = new ProcessBuilder("gcc", "-o", "out", "out.s").inheritIO().start();
		gcc.waitFor();

		Process run = new ProcessBuilder("./out").redirectErrorStream(true).start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(run.getInputStream()));
		String line;
		while ((line = reader.readLine()) != null) {
			System.out.println(line);
		}
		int result = run.waitFor();
		System.out.println("Exit code: " + (byte) result);
    }
}

