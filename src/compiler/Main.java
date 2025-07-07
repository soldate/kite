// Mini compiler for Kite - Stage 5: block, return, if, while, and gcc+run
package compiler;

import java.io.*;

import compiler.ast.core.*;
import compiler.util.*;

public class Main {

	public static void main(String[] args) throws Exception {
		try {
			String input = args.length > 0 ? args[0] : """
					class A {
						B b;
					}

					class B {
						A a = null;
						int x;
					}

					int main() {
						A a;
						a.b.x = 1;
						return a.b.x;
					}
					""";

			if (args[0] != null && args[0].contains(".kite")) input = Util.loadKiteFile(args[0]);

			Lexer lexer = new Lexer(input);
			Parser parser = new Parser(lexer);
			ProgramNode ast = parser.parse();

			System.err.println("packageName: " + ast.packageName);

			PrintWriter out = new PrintWriter("out.s");
			CodeGen codegen = new CodeGen(out);
			codegen.gen(ast);
			out.close();

			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("win")) {
				// Compile the generated assembly code using tcc
				Process gcc = new ProcessBuilder("tcc.exe", "-o", "out.exe", "-c", "out.s").inheritIO().start();
				gcc.waitFor();

				// ./out; echo "Exit code: $?"
				Process run = new ProcessBuilder(".\\out.exe").redirectErrorStream(true).start();
				BufferedReader reader = new BufferedReader(new InputStreamReader(run.getInputStream()));
				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println(line);
				}
				int result = run.waitFor();
				System.err.println("Exit code: " + (byte) result);

			} else if (os.contains("nux") || os.contains("nix")) {
				// Compile the generated assembly code using gcc
				// gcc -no-pie -g -o out out.s
				Process gcc = new ProcessBuilder("gcc", "-o", "-g", "out", "out.s").inheritIO().start();
				gcc.waitFor();

				// ./out; echo "Exit code: $?"
				Process run = new ProcessBuilder("./out").redirectErrorStream(true).start();
				BufferedReader reader = new BufferedReader(new InputStreamReader(run.getInputStream()));
				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println(line);
				}
				int result = run.waitFor();
				System.err.println("Exit code: " + (byte) result);
			}

		} catch (Exception e) {
			Util.debugPrintTokens(Parser.current, 5, e);
		}
	}
}
