/**
 * Created: 19.02.15 19:56
 */
package com.fiftin;

import java.util.Map;
import java.util.Objects;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */
public class Pair<F, S> {
    public final F first;
    public final S second;

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public static <A, B> Pair<A, B> create(A a, B b) {
        return new Pair<>(a, b);
    }

    public static <A, B> Pair<A, B> create(Map.Entry<A, B> e) {
        return new Pair<>(e.getKey(), e.getValue());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Pair)) {
            return false;
        }
        Pair<?, ?> pair = (Pair<?, ?>) obj;
        return Objects.equals(pair.first, first) && Objects.equals(pair.second, second);
    }

    @Override
    public int hashCode() {
        return (first == null ? 0 : first.hashCode()) ^ (second == null ? 0 : second.hashCode());
    }

    public F getKey() {
        return first;
    }

    public S getValue() {
        return second;
    }
}
