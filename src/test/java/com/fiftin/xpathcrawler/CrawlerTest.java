/**
 * Created: 19.02.15 19:35
 */
package com.fiftin.xpathcrawler;

import com.fiftin.*;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */
public class CrawlerTest {

    @Test
    public void testMapToCsv() throws IOException {
        final ArrayList<Pair<String, List<String>>> map = new ArrayList<>();
        final StringWriter writer = new StringWriter();
        map.add(Pair.create("Address", Arrays.asList("Lenin st. 43-248")));
        map.add(Pair.create("Name", Arrays.asList("James")));
        map.add(Pair.create("Phone", Arrays.asList("+790813744", "+137842803")));
        map.add(Pair.create("Email", Arrays.asList("james.parker@gmail.com", "jimmy@yahoo.com", "jj@live.ru")));
        CSV.writeCsv(Arrays.<String>asList("Address", "Name", "Phone", "Email"), writer);
        CSV.writeCsv(map, writer);
        Assert.assertEquals("\"Address\",\"Name\",\"Phone\",\"Email\"\n" +
                "\"Lenin st. 43-248\",\"James\",\"+790813744\",\"james.parker@gmail.com\"\n" +
                ",,\"+137842803\",\"jimmy@yahoo.com\"\n" +
                ",,,\"jj@live.ru\"\n" +
                ",,,", writer.toString());
    }

    @Test
    public void testCrawl() throws IOException, XPathExpressionException, URISyntaxException {
        final Path configPath = Paths.get(CrawlerTest.class.getResource("test-config-1.json").toURI());
        final String conf = new String(Files.readAllBytes(configPath));
        final JSONObject config = new JSONObject(conf);
        final XPathFactory xPathFactory = XPathFactory.newInstance();
        final JSONObject jContent = config.getJSONObject("content");
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final OutputStreamWriter writer = new OutputStreamWriter(out);
        final Crawler crawler = new Crawler(new CSVWriter(writer), xPathFactory);
        final Document doc = XML.loadDocument(CrawlerTest.class.getResource("test-doc-1.html"));
        crawler.crawlDocument(doc, jContent, Arrays.asList("Email", "Address", "Phone", "Fax"), new HashMap<>(), new ArrayList<>());
        writer.flush();
        final String s = out.toString();
        Assert.assertEquals(
                //"\"Email\", \"Address\", \"Phone\", \"Fax\"\n" +
                "\"test@yandex.com\",\"Lenin st. 100-100\",\"+1738938483\",\"+222939372\"\n" +
                "\"test@gmail.com\",,,\"+726352512\"\n" +
                "\"test@test.com\",,,\"+85394499444\"\n" +
                ",,,\n", s);
    }

    @Test
    public void testCrawl2() throws IOException, XPathExpressionException, URISyntaxException {

        final Path configPath = Paths.get(CrawlerTest.class.getResource("test-config-2.json").toURI());
        final String conf = new String(Files.readAllBytes(configPath));
        final JSONObject config = new JSONObject(conf);
        final XPathFactory xPathFactory = XPathFactory.newInstance();
        final JSONObject jContent = config.getJSONObject("content");
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final OutputStreamWriter writer = new OutputStreamWriter(out);
        final Crawler crawler = new Crawler(new CSVWriter(writer), xPathFactory);
        final Document doc = XML.loadDocument(CrawlerTest.class.getResource("test-doc-1.html"));
        crawler.crawlDocument(doc, jContent, Util.toStringArray(config.getJSONArray("content-order")), new HashMap<>(), new ArrayList<>());
        writer.flush();
        final String s = out.toString();
        Assert.assertEquals(
                "\"-\",\"+1738938483\",\"Lenin st. 100-100\",\"test@yandex.com\"\n" +
                ",,,\"test@gmail.com\"\n" +
                ",,,\"test@test.com\"\n" +
                ",,,\n", s);
    }

    @Test
    public void testGetURL() throws MalformedURLException {
        final URL url = Util.getURL(new URL("http://www.cdga.org/ratings.asp?id=43&pid=6"), "clubs.asp?cmd=&amp;cid=40&amp;show=rate");
        Assert.assertEquals("http://www.cdga.org/clubs.asp?cmd=&amp;cid=40&amp;show=rate", url.toString());
    }

    @Test
    public void testCrawl3() throws IOException, XPathExpressionException, URISyntaxException {

        final Path configPath = Paths.get(CrawlerTest.class.getResource("test-config-3.json").toURI());
        final String conf = new String(Files.readAllBytes(configPath));
        final JSONObject config = new JSONObject(conf);
        final XPathFactory xPathFactory = XPathFactory.newInstance();
        final JSONObject jContent = config.getJSONObject("content");
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final OutputStreamWriter writer = new OutputStreamWriter(out);
        final Crawler crawler = new Crawler(new CSVWriter(writer), xPathFactory);
        final Document doc = XML.loadDocument(CrawlerTest.class.getResource("test-doc-3.html"));
        crawler.crawlDocument(doc, jContent, Arrays.asList("Address"), new HashMap<>(), new ArrayList<>());
        writer.flush();
        final String s = out.toString();
        Assert.assertEquals("\"Address Lenin st. 100-100\"\n" +
                "\"Address K. Marks st. 200-200\"\n" +
                "\"Address Pushkin st. 300-300\"", s.trim());
    }

}
