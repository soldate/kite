package compiler.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class MyPrintWriter extends PrintWriter {

	public MyPrintWriter(String file) throws FileNotFoundException {
		super(file);
	}

	@Override
	public PrintWriter printf(String format, Object... args) {
		System.out.printf(format, args);
		return super.printf(format, args);
	}

	@Override
    public void println(String x) {
        System.out.println(x);
        super.println(x);
    }
}
