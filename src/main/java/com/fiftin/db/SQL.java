/**
 * Created: 27.02.15 16:04
 */
package com.fiftin.db;

import java.util.Collection;
import java.util.List;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */
public class SQL {

    public static String makeUpdateQuery(final String table, final Collection<String> columns, final List<String> filterColumns) {
        final StringBuilder builder = new StringBuilder();
        builder.append("UPDATE ").append(table).append(" SET ");
        boolean first = true;
        for (final String column : columns) {
            if (filterColumns.contains(column))
                continue;
            if (!first)
                builder.append(", ");
            else
                first = false;
            builder.append(column).append(" = ?");
        }
        if (columns.size() > 0) {
            builder.append(" WHERE ");
            for (final String x : filterColumns) {
                builder.append(x).append(" = ?");
            }
        }
        return builder.toString();
    }

    public static String makeSelectQuery(final String table, final List<String> columns, final String primaryKey, final String predicate,
                                         final int limit, final boolean hasIdFilter) {
        final StringBuilder ret = new StringBuilder();
        ret.append("SELECT ");
        boolean first = true;
        for (final String col : columns) {
            if (!first)
                ret.append(", ");
            first = false;
            ret.append(col);
        }
        ret.append(" FROM ").append(table).append(" WHERE ");
        if (!hasIdFilter) {
            ret.append("1 = 1");
        } else {
            ret.append(primaryKey).append(" > ?");
        }
        if (predicate != null && !predicate.isEmpty())
            ret.append(" and (").append(predicate).append(")");
        ret.append(" ORDER BY ").append(primaryKey);
        if (limit > 0)
            ret.append(" LIMIT ").append(limit);
        return ret.toString();
    }

    public static String makeInsertQuery(final String table, final Collection<String> columns, final String primaryKey) {
        final StringBuilder builder = new StringBuilder();
        builder.append("INSERT INTO ").append(table).append("(");
        boolean first = true;
        for (final String column : columns) {
            if (column.equals(primaryKey))
                continue;
            if (!first)
                builder.append(", ");
            else
                first = false;
            builder.append(column);
        }
        builder.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i != 0)
                builder.append(", ");
            builder.append("?");
        }
        builder.append(")");
        return builder.toString();
    }

}
