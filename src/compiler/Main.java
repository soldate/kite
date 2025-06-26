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
		// TODO Arrumar o código para deixar a árvore 
		// com os dados pra gerar código sem gambiarras
		// ver se é útil mesmo "gen" em Parser.
		// Fazer funcionar a.b.x = 1;

class Coord {
    int x;
    int y;
}

class Rect {
    Coord origin;
    Coord size;

    int area() {
        return this.size.x * this.size.y;
    }

    void move(int dx, int dy) {
        this.origin.x = this.origin.x + dx;
        this.origin.y = this.origin.y + dy;
    }

    int isSquare() {
        if (this.size.x == this.size.y) {
            return 1;
        } else {
            return 0;
        }
    }
}

class sumClass {
	int sumTo(int n) {
		int total = 0;
		int i = 1;
		while (i <= n) {
			total = total + i;
			i = i + 1;
		}
		return total;
	}
}

int main() {
    Rect r;
    r.origin.x = 0;
    r.origin.y = 0;
    r.size.x = 4;
    r.size.y = 4;

    int area = r.area();
    int square = r.isSquare();
    r.move(3, 2);

    int sx = r.origin.x;
    int sy = r.origin.y;

    int logic = (1 + 2 == 3) && (4 > 3) || (0 == 1);
    int neg = !0;

	sumClass s;
    int sum = s.sumTo(5); // 1+2+3+4+5 = 15

    return area + square + sx + sy + logic + neg + sum;
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

