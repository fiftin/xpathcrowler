/**
 * Created: 26.02.15 19:11
 */
package com.fiftin.db;

import java.sql.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */
public class DBIterator implements Iterator<Map<String, Object>> {

    public final String table;
    public final List<String> columns;
    public final String predicate;
    public final Connection connection;
    public final String primaryKey;
    public final int limit;

    private ResultSet blockResultSet;
    public Object lastId;
    private boolean hasNext;

    public DBIterator(String table, List<String> columns, final String primaryKey, String predicate, Connection connection, int limit) throws SQLException {
        this.table = table;
        this.columns = columns;
        this.primaryKey = primaryKey;
        this.predicate = predicate;
        this.connection = connection;
        this.limit = limit;
        init();
    }

    private void init() throws SQLException {
        hasNext = queryNextBlock();
    }

    private Map<String, Object> get() throws SQLException {
        final HashMap<String, Object> ret = new HashMap<>();
        for (final String column : columns) {
            ret.put(column, blockResultSet.getObject(column));
        }
        return ret;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    public boolean queryNextBlock() throws SQLException {
        if (hasNext)
            throw new RuntimeException("Block is not finished");
        final String sql = SQL.makeSelectQuery(table, columns, primaryKey, predicate, limit, lastId != null);
        final PreparedStatement statement = connection.prepareStatement(sql);
        if (lastId != null) {
            statement.setObject(1, lastId);
        }
        blockResultSet = statement.executeQuery();
        return blockResultSet.next();
    }

    @Override
    public Map<String, Object> next() {
        try {
            if (!hasNext)
                throw new RuntimeException("No more rows");
            final Map<String, Object> ret = get();
            lastId = ret.get(primaryKey);
            hasNext = blockResultSet.next();
            if (!hasNext) {
                hasNext = queryNextBlock();
            }
            return ret;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
