/**
 * Created: 03.03.15 10:36
 */
package com.fiftin.xpathcrawler;

import com.fiftin.Util;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */
public class UtilTest {

    @Test
    public void testFormat1() {
        final String s = Util.format("Hello, World! - Hello, World! - Test", "before-hyphen");
        Assert.assertEquals("Hello, World!", s);
    }
    @Test
    public void testFormat2() {
        final String s = Util.format("Hello, {naturalKey:url}! Hello, {1}! Hello, {0}!", Util.map(Arrays.asList("naturalKey"), Arrays.asList("Denis Gukov")), Arrays.asList("World", "Galaxy"));
        Assert.assertEquals("Hello, Denis+Gukov! Hello, Galaxy! Hello, World!", s);
    }
    @Test
    public void testFormat3() {
        final String s = Util.format("Hello } ", Util.map(Arrays.asList(), Arrays.asList()), Arrays.asList("World", "Galaxy"));
        Assert.assertEquals("Hello } ", s);
    }
//    @Test
//    public void testFormat4() {
//        try {
//            Util.format("Hello { dddd ", Util.map(Arrays.asList(), Arrays.asList()), Arrays.asList("World", "Galaxy"));
//            Assert.fail();
//        } catch (final RuntimeException e) {
//            Assert.assertEquals("Unexpected end", e.getMessage());
//        }
//    }

    @Test
    public void testFormatNull() {
        final String s = Util.format("Test {myVar}", Util.map(Arrays.asList("myVar"), Arrays.asList((String)null)));
        Assert.assertEquals("Test ", s);
    }
    @Test
    public void testFormat5() {
        final String s = Util.format("Hello, {naturalKey:remove-word:Hello;Hi}!", Util.map(Arrays.asList("naturalKey"), Arrays.asList("Hello Hi World Hello Hi")));
        Assert.assertEquals("Hello,   World  !", s);
    }
    @Test
    public void testFormat6() {
        final String s = Util.format("Hello, {naturalKey:remove-word:Hello World;Hi Galaxy}!", Util.map(Arrays.asList("naturalKey"), Arrays.asList("Hello World = Hi Galaxy Hi Galaxy1")));
        Assert.assertEquals("Hello,  =  Hi Galaxy1!", s);
    }
    @Test
    public void testFormat7() {
        final String s = Util.format("Hello, {naturalKey:remove-word:C & GC}!", Util.map(Arrays.asList("naturalKey"), Arrays.asList("Alasca C & GC")));
        Assert.assertEquals("Hello, Alasca !", s);
    }

    @Test
    public void testFormat8() {
        final String s = Util.format("Hello, {naturalKey:remove-word:&amp}!", Util.map(Arrays.asList("naturalKey"), Arrays.asList("Alasca C & GC")));
        Assert.assertEquals("Hello, Alasca C  GC!", s);
    }


    @Test
    public void testFormat9() {
        final String s = Util.format("Hello, {naturalKey:replace-word:C & G C/Golf Club}!", Util.map(Arrays.asList("naturalKey"), Arrays.asList("Alasca C & G C")));
        Assert.assertEquals("Hello, Alasca Golf Club!", s);
    }

    @Test
    public void testFormat10() {
        final String s = Util.format("Hello, {naturalKey:replace-word:C & G C/Golf Club,remove-word: Hell oow,trim,lower}!", Util.map(Arrays.asList("naturalKey"), Arrays.asList("Alasca C & G C Hell oow")));
        Assert.assertEquals("Hello, alasca golf club!", s);
    }



    @Test
    public void testFormat11() {
        final String s = Util.format("Hello, {0}!", Util.map(Arrays.asList(), Arrays.asList()), "World");
        Assert.assertEquals("Hello, World!", s);
    }

    @Test
    public void testFormat12() {
        final String s = Util.format("Hello, {0}!", Util.map(Arrays.asList(), Arrays.asList()), 12);
        Assert.assertEquals("Hello, 12!", s);
    }


    @Test
    public void testFormat13() {
        final String s = Util.format("Hello, {naturalKey:lower,remove-word:at;the;of;on;&amp,replace-word:g c/golf club;gc/golf club;cc/country club; c c/country club,before-hyphen,trim,url}!", Util.map(Arrays.asList("naturalKey"), Arrays.asList("Hele &  Line GC")));
        Assert.assertEquals("Hello, hele+++line+golf+club!", s);
    }


    @Test
    public void testFormat14() {

        final String s = Util.format("Hello, {naturalKey:lower,remove-word:at;the;of;on;&amp,replace-word:g c/golf club;gc/golf club;cc/country club; c c/country club,before-hyphen,trim,one-space-and-order-words}!", Util.map(Arrays.asList("naturalKey"), Arrays.asList("Hele &  Line GC")));
        Assert.assertEquals("Hello, club golf hele line!", s);
    }


}

