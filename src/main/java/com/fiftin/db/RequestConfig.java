/**
 * Created: 02.03.15 15:58
 */
package com.fiftin.db;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */
public class RequestConfig extends ColumnOwnerConfig {
    public final DBWriterMode mode;
    public final List<String> filters = new ArrayList<>();
    RequestConfig(String table, DBWriterMode mode) {
        super(table);
        this.mode = mode;
    }

    public void loadFilters(final JSONArray filters) {
        for (int i = 0; i < filters.length(); i++) {
            this.filters.add(filters.getString(i));
        }
    }

    public static RequestConfig load(final JSONObject obj) {
        final RequestConfig ret = new RequestConfig(obj.getString("table"), Enum.valueOf(DBWriterMode.class, obj.getString("mode")));
        ret.loadColumns(obj.get("columns"));
        ret.loadFilters(obj.getJSONArray("filters"));
        return ret;
    }
}
