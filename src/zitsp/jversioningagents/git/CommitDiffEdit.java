package codelamp.pluginlamp.internal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class CommitDiffEdit {

	private CommitDiffEdit() {
	}

	private static final String ADDED_LINE = "+";
	private static final String REMOVED_LINE = "-";
	private static final String FROMPATH_LINE = "---";
	private static final String TOPATH_LINE = "+++";

//	private static final int PIPEDOUTPUTSTREAM_BUFFER_SIZE = 512 * 1000;
//	OutputStream editWriter2 = new BufferedOutputStream(editWriter, PIPEDOUTPUTSTREAM_BUFFER_SIZE);
	static boolean tes = false;
	public static boolean extractPrefixedLine(InputStream input, OutputStream output, String prefix) {
		if (input == null || output == null) {
			throw new NullPointerException();
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, Charsets.UTF8));
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, Charsets.UTF8))) {
			while (reader.ready()) {
				String str = reader.readLine();
				if (str == null) {
					break;
				} else if (!str.startsWith(FROMPATH_LINE) && !str.startsWith(TOPATH_LINE) && str.startsWith(prefix)) {
					writer.write(str.substring(prefix.length()));
					writer.newLine();
				} else {
					writer.newLine();
				}
			}
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	public static boolean extractAddedLine(InputStream input, OutputStream output) {
		return extractPrefixedLine(input, output, ADDED_LINE);
	}
	public static boolean extractRemovedLine(InputStream input, OutputStream output) {
		return extractPrefixedLine(input, output, REMOVED_LINE);
	}
}