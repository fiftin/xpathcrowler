/**
 * Created: 24.02.15 18:42
 */
package com.fiftin.db;

import org.json.JSONObject;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */
public class TableConfig extends ColumnOwnerConfig {

    public final String foreignKey;
    public final String foreignKeyTable;
    public final String primaryKey;

    TableConfig(String table, String foreignKey, String foreignKeyTable, String primaryKey) {
        super(table);
        this.foreignKey = foreignKey;
        this.foreignKeyTable = foreignKeyTable;
        this.primaryKey = primaryKey;
    }

    public static TableConfig load(JSONObject obj) {
        final TableConfig ret = new TableConfig(obj.getString("naturalKey"),
                obj.has("foreign-key") ? obj.getString("foreign-key") : null,
                obj.has("foreign-key-table") ? obj.getString("foreign-key-table") : null,
                obj.has("primary-key") ? obj.getString("primary-key") : null);
        ret.loadColumns(obj.get("columns"));
        return ret;
    }
}
