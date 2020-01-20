/**
 * Created: 27.02.15 15:25
 */
package com.fiftin.xpathcrawler;

import acolyte.jdbc.QueryResult;
import acolyte.jdbc.RowList;

import java.sql.SQLWarning;
import java.util.List;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */

public class MyQueryResult implements QueryResult {

    public final SQLWarning warning;
    public final MyRowList rows;
    public final int limit;

    public MyQueryResult(final MyRowList rows, final SQLWarning warning, final int limit) {
        this.warning = warning;
        this.rows = rows;
        this.limit = limit;
    }

    public MyQueryResult(final List<String> labels, final SQLWarning warning, final int limit) {
        this.warning = warning;
        this.rows = new MyRowList(labels);
        this.limit = limit;
    }

    @Override
    public RowList<?> getRowList() {
        return rows;
    }

    @Override
    public QueryResult withWarning(SQLWarning warning) {
        return new MyQueryResult(this.rows, warning, limit);
    }

    @Override
    public QueryResult withWarning(String reason) {
        return withWarning(new SQLWarning(reason));
    }

    @Override
    public SQLWarning getWarning() {
        return warning;
    }
}
