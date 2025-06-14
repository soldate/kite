// Mini compiler for Kite - Stage 5: block, return, if, while, and gcc+run
package compiler;

import java.io.PrintWriter;

import compiler.ast.Node;
import compiler.util.MyPrintWriter;

// === MAIN ===
public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java -cp bin compiler.Main \"{ int x = 0; while (x) return 1; return 0; }\"");
            System.exit(1);
        }

        String input = args[0];
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Node ast = parser.parse();

        try (PrintWriter out = new MyPrintWriter("out.s")) {
            CodeGen gen = new CodeGen(out);
            gen.emit(ast);
        }

        Process gcc = new ProcessBuilder("gcc", "-o", "out", "out.s").inheritIO().start();
        int gccStatus = gcc.waitFor();
        if (gccStatus != 0) {
            System.err.println("GCC compilation failed.");
            System.exit(1);
        }

        Process run = new ProcessBuilder("./out").start();
        int exitCode = run.waitFor();
        System.out.println("Program exited with code: " + exitCode);
    }
}
