package com.fiftin;

import java.util.Collection;
import java.util.List;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */
public interface MyWriter {
    default void auth() {}
    void writeHeader(final Collection<String> columnNames);
    int write(List<Pair<String, List<String>>> content);
}
