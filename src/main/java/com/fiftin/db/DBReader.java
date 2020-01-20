/**
 * Created: 26.02.15 18:42
 */
package com.fiftin.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */
public class DBReader implements Iterable<Map<String, Object>> {

    public final String table;
    public final List<String> columns;
    public final String predicate;
    public final String primaryKey;
    public final Connection connection;
    public final int limit;

    public DBReader(String table, List<String> columns, final String primaryKey, String predicate,
                    String connectionString, String user, String password, int limit) throws SQLException {
        this.table = table;
        this.columns = columns;
        this.primaryKey = primaryKey;
        this.predicate = predicate;
        this.limit = limit;
        connection = DriverManager.getConnection(connectionString, user, password);
    }

    @Override
    public Iterator<Map<String, Object>> iterator() {
        try {
            return new DBIterator(table, columns, primaryKey, predicate, connection, limit);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
