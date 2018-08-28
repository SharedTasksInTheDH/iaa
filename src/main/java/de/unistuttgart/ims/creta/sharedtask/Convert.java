package de.unistuttgart.ims.creta.sharedtask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.uima.UIMAException;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

public class Convert {

	public static void main(String[] args) throws UIMAException, FileNotFoundException, IOException {
		Options result = CliFactory.parseArguments(Options.class, args);
		CatmaTei2CSV c = new CatmaTei2CSV(result.getInput());
		if (result.getOutput() == null) {
			c.setAppendable(System.out);
		} else {
			c.setAppendable(new FileWriter(getCSVFile(result)));
			c.setMarkdownFile(getMDFile(result));
			c.setLatexFile(getTEXFile(result));
		}
		c.setAnnotatorId(result.getAnnotatorId());

		c.process();
	}

	public static File getCSVFile(Options options) {
		return new File(options.getOutput().getAbsolutePath() + ".csv");
	}

	public static File getMDFile(Options options) {
		return new File(options.getOutput().getAbsolutePath() + ".md");
	}

	public static File getTEXFile(Options options) {
		return new File(options.getOutput().getAbsolutePath() + ".tex");
	}

	interface Options {
		@Option
		File getInput();

		@Option(defaultToNull = true)
		File getOutput();

		@Option(defaultToNull = true)
		String getAnnotatorId();
	}
}
