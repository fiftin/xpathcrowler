/**
 * Created: 26.02.15 18:51
 */
package com.fiftin;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */
public class CSVWriter implements MyWriter {
    private int totalCount = 0;
    private int count = 0;
    private long startMillis = System.currentTimeMillis();

    public final Writer writer;

    public CSVWriter(Writer writer) {
        this.writer = writer;
    }

    public void writeHeader(Collection<String> columnNames) {
        try {
            CSV.writeCsv(columnNames, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int write(List<Pair<String, List<String>>> content) {
        totalCount++;
        count++;
        System.out.println("Written " + count + " records by " + ((System.currentTimeMillis() - startMillis) / 1000) + " seconds");


//        if (count > 300) {
//            try {
//                Thread.sleep(1000 * 60 * 10);
//                count = 0;
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }

        try {
            final int ret = CSV.writeCsv(content, writer);
            writer.append("\n");
            writer.flush();
            return ret;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
