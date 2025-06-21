// Mini compiler for Kite - Stage 5: block, return, if, while, and gcc+run
package compiler;

import java.io.*;

import compiler.ast.BlockNode;

public class Main {
    public static void main(String[] args) throws Exception {
		String input = args.length > 0 ? args[0]
				: 
		"""		
		struct Point {
			int x;
			int y;
		}

		int main() {
			Point p;
			p.x = 2;
			p.y = 3;
			return p.x + p.y;
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
		System.out.println("Exit code: " + (byte) result);
    }
}

