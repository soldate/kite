package kite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class BinaryIntegrationTest {
    private final Compiler compiler = new Compiler();

    @TempDir
    Path tempDir;

    @Test
    void helloExampleRuns() throws IOException, InterruptedException {
        assertExampleOutput("hello.kite", "hello");
    }

    @Test
    void controlExampleRuns() throws IOException, InterruptedException {
        assertExampleOutput("control.kite", "tickonetick");
    }

    @Test
    void forExampleRuns() throws IOException, InterruptedException {
        assertExampleOutput("for.kite", "looplooploop");
    }

    @Test
    void typesExampleRuns() throws IOException, InterruptedException {
        assertExampleOutput("types.kite", "types");
    }

    @Test
    void arraysExampleRuns() throws IOException, InterruptedException {
        assertExampleOutput("arrays.kite", "arrays");
    }

    @Test
    void objectReferenceExampleCompilesAndRuns() throws IOException, InterruptedException {
        assertExampleOutput("references.kite", "");
    }

    @Test
    void pointerExampleCompilesAndRuns() throws IOException, InterruptedException {
        assertExampleOutput("pointers.kite", "");
    }

    private void assertExampleOutput(String fileName, String expectedOutput) throws IOException, InterruptedException {
        Path cFile = tempDir.resolve(fileName.replace(".kite", ".c"));
        Path binary = tempDir.resolve(fileName.replace(".kite", ""));
        Files.writeString(cFile, compileExample(fileName));

        run(List.of("gcc", cFile.toString(), "-o", binary.toString()));
        ProcessResult result = run(List.of(binary.toString()));

        assertEquals(expectedOutput, result.stdout());
    }

    private String compileExample(String fileName) throws IOException {
        String source = Files.readString(Path.of("examples", fileName));
        return compiler.compile(source);
    }

    private ProcessResult run(List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).start();
        String stdout = new String(process.getInputStream().readAllBytes());
        String stderr = new String(process.getErrorStream().readAllBytes());
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            fail("Command failed (" + exitCode + "): " + String.join(" ", command) + "\n" + stderr);
        }

        return new ProcessResult(stdout, stderr);
    }

    private record ProcessResult(String stdout, String stderr) {
    }
}
