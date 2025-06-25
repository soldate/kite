// Mini compiler for Kite - Stage 5: block, return, if, while, and gcc+run
package compiler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import compiler.ast.ProgramNode;

public class Main {
    public static void main(String[] args) throws Exception {
		String input = args.length > 0 ? args[0]: 
		"""		
		int main() {
			int x = 0;
			int y = 0;
			while (x < 3) {
				x = x + 1;
				y = y + x;
			}
			return y;
		}
		""";
		
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        ProgramNode ast = parser.parse();

		PrintWriter out = new PrintWriter("out.s");
		CodeGen codegen = new CodeGen(out);
		codegen.gen(ast);
		out.close();

        Process gcc = new ProcessBuilder("gcc", "-o", "out", "out.s").inheritIO().start();
		gcc.waitFor();

		Process run = new ProcessBuilder("./out").redirectErrorStream(true).start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(run.getInputStream()));
		String line;
		while ((line = reader.readLine()) != null) {
			System.out.println(line);
		}
		int result = run.waitFor();
		System.err.println("Exit code: " + (byte) result);
    }
}

