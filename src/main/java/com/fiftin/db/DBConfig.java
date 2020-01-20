/**
 * Created: 24.02.15 18:45
 */
package com.fiftin.db;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */
public class DBConfig {

    public final String connectionString;
    public final List<TableConfig> tables = new ArrayList<>();
    public final List<RequestConfig> requests = new ArrayList<>();
    public final String user;
    public final String password;

    public DBConfig(String connectionString, String user, String password) {
        this.connectionString = connectionString;
        this.user = user;
        this.password = password;
    }

    public static DBConfig load(final JSONObject obj) {
        final DBConfig ret = new DBConfig(obj.getString("connection-string"), obj.getString("user"), obj.getString("password"));
        if (obj.has("tables")) {
            final JSONArray tables = obj.getJSONArray("tables");
            for (int i = 0; i < tables.length(); i++) {
                ret.tables.add(TableConfig.load(tables.getJSONObject(i)));
            }
        }
        if (obj.has("requests")) {
            final JSONArray requests = obj.getJSONArray("requests");
            for (int i = 0; i < requests.length(); i++) {
                ret.requests.add(RequestConfig.load(requests.getJSONObject(i)));
            }
        }
        return ret;
    }

    public TableConfig getTableConfig(final String tableName) {
        for (final TableConfig x : tables) {
            if (x.table.equals(tableName))
                return x;
        }
        throw new RuntimeException("Table configuration not found");
    }
}
