/**
 * Created: 13.03.15 10:37
 */
package com.fiftin.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */
public class DBUtil {

    public static Map<String, Object> readRow(final ResultSet set, final String[] columns) throws SQLException {
        final HashMap<String, Object> ret = new HashMap<>();
        for (final String col : columns) {
            ret.put(col, set.getObject(col));
        }
        return ret;
    }

    public static void fillPreparedStatement(final PreparedStatement statement, final Map<String, Object> fields, final Collection<String> columns) throws SQLException {
        int i = 0;
        for (final String col : columns) {
            i++;
            final Object val = fields.get(col);
            statement.setObject(i, val);
        }
    }

}
