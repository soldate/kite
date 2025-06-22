// Mini compiler for Kite - Stage 5: block, return, if, while, and gcc+run
package compiler;

import java.io.*;

import compiler.ast.BlockNode;

public class Main {
    public static void main(String[] args) throws Exception {
		String input = args.length > 0 ? args[0]
				: 
		"""		
		void ping() {
			int x = 1;
			if (x == 1) return;
			x = 2;
		}

		int main() {
			ping();
			return 42;
		}
		""";
				;
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        BlockNode ast = (BlockNode) parser.parse();

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

