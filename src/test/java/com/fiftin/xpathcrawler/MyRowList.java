/**
 * Created: 27.02.15 15:25
 */
package com.fiftin.xpathcrawler;

import acolyte.jdbc.RowList;
import com.fiftin.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */

public class MyRowList extends RowList<MyRow> {

    public final ArrayList<MyRow> rows = new ArrayList<>();
    public final ArrayList<Class<?>> classes = new ArrayList<>();
    public final List<String> labels;

    public MyRowList(final List<String> labels) {
        this.labels = labels;
    }

    @Override
    public List<MyRow> getRows() {
        return rows;
    }

    @Override
    protected RowList<MyRow> append(MyRow row) {
        if (!rows.add(row))
            throw new RuntimeException();
        if (rows.size() == 1) {
            for (final Object cell : rows.get(0).cells) {
                labels.add("");
                classes.add(cell.getClass());
            }
        }
        return this;
    }

    @Override
    public RowList<MyRow> withLabel(int columnIndex, String label) {
        labels.set(columnIndex, label);
        return this;
    }

    @Override
    public RowList<MyRow> withNullable(int columnIndex, boolean nullable) {
        return this;
    }

    @Override
    public List<Class<?>> getColumnClasses() {
        return classes;
    }

    @Override
    public Map<String, Integer> getColumnLabels() {
        return Util.map(labels, Util.range(1, labels.size()));
    }

    @Override
    public Map<Integer, Boolean> getColumnNullables() {
        return new HashMap<>();
    }
}
