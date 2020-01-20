/**
 * Created: 27.02.15 15:25
 */
package com.fiftin.xpathcrawler;

import acolyte.jdbc.Row;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */

public class MyRow implements Row {
    public final ArrayList<Object> cells = new ArrayList<>();

    public <T> MyRow(final List<T> cells) {
        this.cells.addAll(cells);
    }

    @Override
    public List<Object> cells() {
        return cells;
    }
}
