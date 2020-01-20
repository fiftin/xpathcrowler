/**
 * Created: 26.02.15 19:00
 */
package com.fiftin.db;

import com.fiftin.MyWriter;
import com.fiftin.Pair;
import com.fiftin.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */
public class DBWriter implements MyWriter {

    public final DBWriterMode mode;
    public final String table;
    public final Map<String, ColumnConfig> columns;
    public final List<String> filterColumns;

    public final Connection connection;

    public DBWriter(String table, Map<String, ColumnConfig> columns, List<String> filterColumns, DBWriterMode mode, Connection connection) {
        this.mode = mode;
        this.table = table;
        this.columns = columns;
        this.filterColumns = filterColumns;
        this.connection = connection;
    }

    @Override
    public void writeHeader(Collection<String> columns) { }

    public int insert(final List<Pair<String, List<String>>> content) throws SQLException {
        final List<String> columns = Util.getKeys(content);
        int i = 0;
        while (true) {
            final ArrayList<Object> values = new ArrayList<>();
            boolean end = true;
            for (final Pair<String, List<String>> x : content) {
                if (x.second.size() > i) {
                    values.add(x.second.get(i));
                    end = false;
                } else if (i == 0) {
                    values.add(null);
                }
            }
            if (end)
                break;
            final String sql = SQL.makeInsertQuery(table, columns, null);
            final PreparedStatement statement = connection.prepareStatement(sql);
            for (int k = 0; k < values.size(); k++) {
                statement.setObject(k + 1, values.get(k));
            }
            statement.execute();
            i++;
        }
        return i;
    }

    public int update(final List<Pair<String, List<String>>> content) throws SQLException {
        final List<String> cols = new ArrayList<>();
        final List<String> filterCols = new ArrayList<>();
        int i = 0;
        while (true) {
            final ArrayList<String> values = new ArrayList<>();
            final ArrayList<String> filterValues = new ArrayList<>();
            boolean end = true;
            for (final Pair<String, List<String>> x : content) {
                if (filterColumns.contains(x.first)) {
                    filterValues.add(x.second.size() > i ? x.second.get(i) : null);
                    filterCols.add(x.first);
                    continue;
                }
                cols.add(x.first);
                if (x.second.size() > i) {
                    values.add(x.second.get(i));
                    end = false;
                } else if (i == 0) {
                    values.add(null);
                }
            }
            values.addAll(filterValues);
            cols.addAll(filterCols);
            if (end)
                break;

            final String sql = SQL.makeUpdateQuery(table, cols, filterColumns);
            final PreparedStatement statement = connection.prepareStatement(sql);
            for (int k = 0; k < values.size(); k++) {
                final Object val = columns.get(cols.get(k)).valueOf(values.get(k));
                statement.setObject(k + 1, val);
            }

            statement.execute();
            i++;
        }
        return i;
    }

    @Override
    public int write(final List<Pair<String, List<String>>> content) {
        try {
            switch (mode) {
                case INSERT:
                    return insert(content);
                case UPDATE:
                    return update(content);
                case UPDATE_OR_INSERT:
                    int n = update(content);
                    if (n <= 0)
                        insert(content);
                default:
                    throw new RuntimeException("Unknown mode");
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

}
