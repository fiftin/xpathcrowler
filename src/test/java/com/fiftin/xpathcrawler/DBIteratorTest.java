/**
 * Created: 27.02.15 10:01
 */
package com.fiftin.xpathcrawler;

import acolyte.jdbc.*;
import com.fiftin.Util;
import com.fiftin.db.DBReader;
import com.fiftin.db.SQL;
import org.junit.Assert;
import org.junit.Test;

import java.sql.SQLException;
import java.util.*;
import java.util.function.Predicate;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */
public class DBIteratorTest {

    public void registerMockDriver() {
        final acolyte.jdbc.StatementHandler handler = new acolyte.jdbc.CompositeHandler().withQueryDetection("^SELECT").withQueryHandler(new AbstractCompositeHandler.QueryHandler() {
            final ArrayList<MyRow> rows = new ArrayList<>();

            {
                rows.add(new MyRow(Arrays.asList("Denis", "denguk@gmail.com", 3447)));
                rows.add(new MyRow(Arrays.asList("Igor", "igor.s@gmail.com", 4565)));
                rows.add(new MyRow(Arrays.asList("Elena", "el222.s@gmail.com", 22222)));
                rows.add(new MyRow(Arrays.asList("John", "joo10.s@gmail.com", 333)));
                rows.add(new MyRow(Arrays.asList("I", "iii.s@gmail.com", 23232)));
                rows.add(new MyRow(Arrays.asList("JZ", "jz.s@gmail.com", 234233)));
                rows.add(new MyRow(Arrays.asList("JD", "jd.s@gmail.com", 2343)));
            }

            @Override
            public QueryResult apply(String sql, List<StatementHandler.Parameter> parameters) throws SQLException {
                final MyQueryResult ret = new MyQueryResult(Arrays.asList("login", "email", "passport"), null, 3);
                final ArrayList<MyRow> newRows = new ArrayList<>(rows);
                newRows.sort(new Comparator<MyRow>() {
                    @Override
                    public int compare(MyRow o1, MyRow o2) {
                        return ((String) o1.cells.get(0)).compareTo((String) o2.cells.get(0));
                    }
                });
                if (parameters.size() == 0) {
                    for (int i = newRows.size() - 1; i >= 3; i--) {
                        newRows.remove(i);
                    }
                } else {
                    final String login = (String) parameters.get(0).getValue();
                    newRows.removeIf(new Predicate<MyRow>() {
                        @Override
                        public boolean test(MyRow myRow) {
                            return ((String) myRow.cells.get(0)).compareTo(login) != 1;
                        }
                    });
                }
                ret.rows.rows.addAll(newRows);
                return ret;
            }
        });
        acolyte.jdbc.Driver.register("my-unique-id", handler);
    }

    @Test
    public void testGetSQL() throws SQLException {
        final String sql = SQL.makeSelectQuery("user", Arrays.asList("login", "email", "passport"), "login", null, 3, false);
        Assert.assertEquals("SELECT login, email, passport FROM user WHERE 1 = 1 ORDER BY login LIMIT 3", sql);
    }

    @Test
    public void testQuery() throws SQLException {
        registerMockDriver();
        final DBReader reader = new DBReader("user", Arrays.asList("login", "email", "passport"), "login", null, "jdbc:acolyte:anything-you-want?handler=my-unique-id", "", "", 3);
        for (final Map<String, Object> x : reader) {
            Util.print(x);
        }
    }
}
