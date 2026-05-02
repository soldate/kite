package kite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: kitec <file.kite>");
            System.exit(64);
        }

        try {
            String source = Files.readString(Path.of(args[0]));
            List<Token> tokens = new Lexer(source).scanTokens();
            Ast.Program program = new Parser(tokens).parse();
            String c = new CGenerator().generate(program);
            System.out.print(c);
        } catch (KiteException e) {
            System.err.println(e.getMessage());
            System.exit(65);
        } catch (IOException e) {
            System.err.println("Could not read file: " + e.getMessage());
            System.exit(66);
        }
    }
}
