/**
 * Created: 25.02.15 12:46
 */
package com.fiftin.db;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */
public class ColumnConfig {
    public final String name;
    public final String type;
    public final Object value;

    public ColumnConfig(String name, String type, Object value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public Object valueOf(final String s) {
        if (type.equals("string"))
            return s;
        if (type.equals("int"))
            return Integer.valueOf(s);
        if (type.equals("float"))
            return Float.parseFloat(s);
        if (type.equals("const"))
            return value;
        throw new RuntimeException("Unknown column type");
    }
}
