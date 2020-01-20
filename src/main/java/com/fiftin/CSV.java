/**
 * Created: 20.02.15 14:30
 */
package com.fiftin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */
public class CSV {

    private CSV() { }
    /**
     *
     * @param content
     * @param csvWriter
     * @throws IOException
     */
    public static void writeCsv(final Collection<String> content, final Writer csvWriter) throws IOException {
        boolean first = true;
        for (final String x : content) {
            first = Util.appendNotFirst(",", csvWriter, first);
            csvWriter.append("\"").append(x.replace("\"", "\"\"")).append("\"");
        }
        csvWriter.append("\n");
    }
    /**
     * Save received key-value pairs to CSV format.
     * @param content
     * @param csvWriter
     * @throws IOException
     */
    public static int writeCsv(List<Pair<String, List<String>>> content, final Writer csvWriter) throws IOException {
        int i = 0;
        while (true) {
            boolean first = true;
            boolean end = true;
            for (final Pair<String, List<String>> x : content) {
                first = Util.appendNotFirst(",", csvWriter, first);
                if (x.getValue().size() > i) {
                    csvWriter.append("\"").append(x.getValue().get(i).replace("\"", "\"\"")).append("\"");
                    end = false;
                } else if (i == 0) {
                    csvWriter.append("\"-\"");
                }
            }
            if (end)
                break;
            csvWriter.append("\n");
            i++;
        }
        return i;
    }

    private static enum CsvState {
        VALUE,
        STRING,
        AFTER_COMMA,
        AFTER_STRING;
        public boolean isValueBody() {
            return this == VALUE || this == STRING;
        }
    }

    public final static char SEPARATOR = ',';

    public static List<String> readRow(final BufferedReader reader) throws IOException {
        return readRow(reader, false, false, SEPARATOR, false);
    }

    public static List<String> readRow(final BufferedReader reader, final boolean spaceIsNull, final boolean ignoreSpaceLines, final char separator, final boolean ignoreQuote) throws IOException {
        ArrayList<String> ret = new ArrayList<String>();
        String line = reader.readLine();
        if (line == null)
            return null;
        if (ignoreSpaceLines && line.trim().isEmpty())
            return ret;
        CsvState state = CsvState.AFTER_COMMA;
        final StringBuilder value = new StringBuilder();
        while (line != null) {
            for (int i = 0; i < line.length(); i++) {
                final char c = line.charAt(i);
                if (c == separator) {
                    if (state == CsvState.STRING) {
                        value.append(c);
                    } else if (state == CsvState.VALUE || state == CsvState.AFTER_COMMA) {
                        ret.add(value.length() == 0 ? null : value.toString());
                        value.setLength(0);
                        state = CsvState.AFTER_COMMA;
                    } else
                        state = CsvState.AFTER_COMMA;
                    continue;
                }
                if (c == '"' && !ignoreQuote) {
                    if (state == CsvState.STRING) {
                        if (line.length() > i + 1 && line.charAt(i + 1) == '"') {
                            value.append('"');
                            i++;
                        } else {
                            state = CsvState.AFTER_STRING;
                            ret.add(value.toString());
                            value.setLength(0);
                        }
                    } else if (state == CsvState.AFTER_COMMA) {
                        state = CsvState.STRING;
                        value.setLength(0);
                    } else {
                        throw new RuntimeException("Illegal character: " + c + ", state: " + state + " line: " + line);
                    }
                    continue;
                }
                switch (c) {
                    //case '"':
                    case ' ':
                    case '\t':
                        if (state.isValueBody()) {
                            value.append(c);
                        } else if (state == CsvState.AFTER_COMMA && !spaceIsNull) {
                            value.append(c);
                        }
                        break;
                    default:
                        if (state.isValueBody()) {
                            value.append(c);
                        } else if (state == CsvState.AFTER_COMMA) {
                            state = CsvState.VALUE;
                            value.append(c);
                        }
                        break;
                }
            }
            if (state == CsvState.AFTER_COMMA) {
                ret.add(value.length() > 0 ? value.toString() : null);
                break;
            } else if (state == CsvState.VALUE) {
                ret.add(value.toString());
                break;
            } else if (state == CsvState.AFTER_STRING) {
                break;
            } else if (state == CsvState.STRING) {
                value.append("\n");
            }
            line = reader.readLine();
        }
        if (state == CsvState.STRING)
            throw new RuntimeException("Expected \"");
        return ret;
    }
}
