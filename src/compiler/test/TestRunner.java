package compiler.test;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;

public class TestRunner {
    public static void main(String[] args) throws Exception {
        File testDir = new File("tests");
        File[] testFiles = testDir.listFiles((dir, name) -> name.endsWith(".kite"));

        if (testFiles == null) {
            System.err.println("No test files found.");
            return;
        }

        Arrays.sort(testFiles, Comparator.comparingInt(f -> extractTestNumber(f.getName())));

        for (File file : testFiles) {
            String code = new String(Files.readAllBytes(file.toPath()));
            String name = file.getName();
            int expected = extractExpectedExitCode(name);

            System.out.printf("Running %s... ", name);

            // 1. Gerar assembly com o compilador Kite
            Process compile = new ProcessBuilder("java", "-cp", "bin", "compiler.Main", code)
                    .redirectOutput(new File("out.s"))
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
            compile.waitFor();

            // 2. Compilar com gcc
            Process gcc = new ProcessBuilder("gcc", "-o", "out", "out.s")
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
            gcc.waitFor();

            // 3. Executar o binário
            Process run = new ProcessBuilder("./out")
                    .start();
            run.waitFor();
            int exit = run.exitValue();

            // 4. Verificar resultado
            if (exit == expected) {
                System.out.println("✅ Passou (exit code " + exit + ")");
            } else {
                System.out.println("❌ Falhou (esperado " + expected + ", obteve " + exit + ")");
            }
        }
    }

    // Exemplo: test3_exit6.kite → retorna 6
    private static int extractExpectedExitCode(String name) {
        try {
            int start = name.indexOf("exit") + 4;
            int end = name.lastIndexOf('.');
            return Integer.parseInt(name.substring(start, end));
        } catch (Exception e) {
            return 0;
        }
    }

    private static int extractTestNumber(String filename) {
        // Espera algo como test5_exit3.kite → retorna 5
        try {
            int start = filename.indexOf("test") + 4;
            int end = filename.indexOf("_", start);
            if (start >= 4 && end > start) {
                return Integer.parseInt(filename.substring(start, end));
            }
        } catch (Exception e) {
            // Ignora erros e retorna um número alto (vai pro final)
        }
        return Integer.MAX_VALUE;
    }
    
}
