package compiler.util;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;

// Read Kite test files from the "tests" directory, compile them using the Kite compiler.
// Then call 'gcc' to mount/compile the assembler code.
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

            Process compile = new ProcessBuilder("java", "-cp", "bin", "compiler.Main", code)
                    .redirectOutput(new File("out.s"))
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
            compile.waitFor();

            Process gcc = new ProcessBuilder("gcc", "-o", "out", "out.s")
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
            gcc.waitFor();

            Process run = new ProcessBuilder("./out")
                    .start();
            run.waitFor();
            int exit = run.exitValue();

            if (exit == expected) {
                System.out.println("✅ Pass (exit code " + exit + ")");
            } else {
                System.out.println("❌ Error (expected " + expected + ", exit " + exit + ")");
            }
        }
    }

    // Ex: test3_exit6.kite → returns 6
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
        // Ex: test3_exit6.kite → returns 3
        try {
            int start = filename.indexOf("test") + 4;
            int end = filename.indexOf("_", start);
            if (start >= 4 && end > start) {
                return Integer.parseInt(filename.substring(start, end));
            }
        } catch (Exception e) {
        }
        return Integer.MAX_VALUE;
    }
    
}
