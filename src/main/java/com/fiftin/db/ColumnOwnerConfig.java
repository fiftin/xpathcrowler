/**
 * Created: 02.03.15 16:11
 */
package com.fiftin.db;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */
public class ColumnOwnerConfig {

    public final String table;
    public final Map<String, ColumnConfig> columns = new HashMap<>();

    public ColumnOwnerConfig(String table) {
        this.table = table;
    }

    protected void loadColumns(final Object columnsObj) {
        if (columnsObj instanceof JSONObject) {
            final JSONObject cols = (JSONObject) columnsObj;
            for (final String col : cols.keySet()) {
                this.columns.put(col, getColumnConfig(cols.get(col)));
            }
        } else {
            final JSONArray columns = (JSONArray) columnsObj;
            for (int i = 0; i < columns.length(); i++) {
                this.columns.put(columns.getString(i), getColumnConfig(columns.getString(i)));
            }
        }
    }

    protected static ColumnConfig getColumnConfig(final Object columnObj) {
        if (columnObj instanceof String)
            return new ColumnConfig((String)columnObj, "string", null);
        if (columnObj instanceof JSONArray) {
            final JSONArray arr = (JSONArray) columnObj;
            if (arr.getString(0).equals("const"))
                return new ColumnConfig(null, arr.getString(0), arr.get(1));
            return new ColumnConfig(arr.getString(1), arr.getString(0), null);
        }
        throw new RuntimeException("Illegal column type");
    }

}
