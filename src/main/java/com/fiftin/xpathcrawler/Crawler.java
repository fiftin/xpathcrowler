/**
 * Created: 20.02.15 14:39
 */
package com.fiftin.xpathcrawler;

import com.fiftin.MyWriter;
import com.fiftin.Pair;


import com.fiftin.Util;
import com.fiftin.XML;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */
public class Crawler {

    private final MyWriter writer;
    private final XPathFactory xPathFactory;
    public static final int WAITING_MILLIS = 100;
    public static final int WAITING_MAX_NUMBER = 2;
    private final String sourceEncoding;
    private final String dumpRoot;
    private final boolean needMakeDump;
    private final boolean loadFromDump;
    private final boolean isOnlyDumpRequired;
    private final int delay;
    private long lastWriteMillis = System.currentTimeMillis();

    public Crawler(final MyWriter writer, final XPathFactory xPathFactory, final String sourceEncoding,
                   final String dumpRoot, final boolean needMakeDump, final boolean loadFromDump,
                   final int delay, final boolean isOnlyDumpRequired) {
        this.writer = writer;
        this.xPathFactory = xPathFactory;
        this.sourceEncoding = sourceEncoding;
        this.dumpRoot = dumpRoot;
        this.needMakeDump = needMakeDump;
        this.loadFromDump = loadFromDump;
        this.delay = delay;
        this.isOnlyDumpRequired = isOnlyDumpRequired;
    }

    public Crawler(final MyWriter writer, final XPathFactory xPathFactory, final String sourceEncoding,
                   final String dumpRoot, final boolean needMakeDump, final boolean loadFromDump) {
        this(writer, xPathFactory, sourceEncoding, dumpRoot, needMakeDump, loadFromDump, 1000, false);
    }


    public Crawler(final MyWriter writer, final XPathFactory xPathFactory) {
        this(writer, xPathFactory, "utf-8", null, false, false);
    }

    public String getDumpRootIfNeedMakeDump() {
        return needMakeDump ? dumpRoot : null;
    }

    private Map<String, Object> crawlTransContent(final Node node,
                                                  final JSONObject jContent,
                                                  final Map<String, Object> variables,
                                                  final List<Object> substitutes) throws XPathExpressionException, IOException, JSONException {
        final Map<String, Object> content = new HashMap<>();
        for (Object fieldName : jContent.keySet()) {
            final Object obj = jContent.get((String) fieldName);
            final List<String> values = new ArrayList<>();
            if (obj instanceof String) {
                crawlNode(values, node, (String) obj, false);
            } else if (obj instanceof JSONArray) {
                crawlByArray(values, node, (JSONArray) obj, variables, substitutes, new ArrayList<>(), content);
            } else if (obj instanceof JSONObject) {
                crawlByJSONObject(values, node, (JSONObject) obj);
            }
            for (int i = 0; i < values.size(); i++) {
                values.set(i, values.get(i).trim());
            }
            if (values.size() > 1) {
                content.put((String) fieldName, values);
            } else if (values.size() > 0) {
                content.put((String) fieldName, values.get(0));
            }
        }
        return content;
    }

    private Document loadDoc(final URL sourceUrl) throws InterruptedException {
        Document source = null;
        int waitingNo = 0;
        while (true) {
            try {
                final URL correctedUrl = loadFromDump ? Util.changeProtocolToFileIfNotExists(sourceUrl, dumpRoot) : sourceUrl;
                source = XML.loadDocument(correctedUrl, sourceEncoding, getDumpRootIfNeedMakeDump());
                waitingNo = 0;
                break;
            } catch (IOException ex) {
                print("Error: " + ex.getMessage() + ", waiting");
                Thread.sleep(WAITING_MILLIS);
                waitingNo++;
                if (waitingNo >= WAITING_MAX_NUMBER) {
                    break;
                }
            } catch (NullPointerException | IllegalArgumentException ex) {
                System.out.println("Error: " + ex.getMessage());
                break;
            }
        }

        return source;
    }

    /**
     *
     * @param sourceUrls Array of URLs for crawling.
     * @param jUrl JSON object with configuration.
     * @param writeHeader Indicate whether need write a header.
     * @param started Indicate whether crawling is started.
     * @param variables
     * @param substitutes
     * @param crawlAllUrls
     * @return Crawling is started or not.
     * @throws IOException
     * @throws XPathExpressionException
     * @throws InterruptedException
     */
    protected boolean crawlRecursive(final URL[] sourceUrls,
                                     final JSONObject jUrl,
                                     final boolean writeHeader,
                                     boolean started,
                                     final Map<String, Object> variables,
                                     final List<Object> substitutes,
                                     final boolean crawlAllUrls) throws IOException, XPathExpressionException, InterruptedException, JSONException {

        for (URL sourceUrl : new LinkedHashSet<>(Arrays.asList(sourceUrls))) {
            print("Recursive: " + sourceUrl);
            final String path = jUrl.getString("link-path");
            final XPath xPath = xPathFactory.newXPath();

            final Document source = loadDoc(sourceUrl);
            if (source == null) {
                if (crawlAllUrls) {
                    continue;
                } else {
                    return started;
                }
            }

            final NodeList urlNodes = (NodeList) xPath.evaluate(path, source, XPathConstants.NODESET);

            final ArrayList<URL[]> urls = new ArrayList<>();
            final Map<URL[], Element> urlElements = new HashMap<>();
            boolean hasSkippedUrls = false;

            // Convert collected link nodes to list of URLs.
            for (int i = 0; i < urlNodes.getLength(); i++) {
                final Element urlElement = (Element) urlNodes.item(i);
                final String rawUrl = urlElement.getAttribute("href");
                if (jUrl.has("link-condition") && !isValidLink(rawUrl, jUrl.getJSONArray("link-condition"))) {
                    print("Skipped link: " + rawUrl);
                    hasSkippedUrls = true;
                    continue;
                }

                final URL url = Util.getURL(sourceUrl, rawUrl);

                if (Util.containsURL(urls, url)) {
                    continue;
                }

                final URL[] tmp;
                if (jUrl.has("override-url")) {
                    if (jUrl.has("start-index") && !jUrl.has("end-index") || !jUrl.has("start-index") && jUrl.has("end-index")) {
                        throw new RuntimeException("start-index or end-index isn't specified");
                    }
                    if (jUrl.has("start-index")) {
                        final List<URL> urlArr = urlToRange(rawUrl, sourceUrl, jUrl, variables);
                        tmp = urlArr.toArray(new URL[urlArr.size()]);
                    } else {
                        final String tmpUrl = overrideString(rawUrl, variables, 0, jUrl.get("override-url"));
                        tmp = new URL[] { Util.getURL(sourceUrl, tmpUrl) };
                    }
                } else {
                    tmp = new URL[] {url};
                }
                urls.add(tmp);
                if (jUrl.has("trans-content")) {
                    urlElements.put(tmp, XML.getParent(urlElement, jUrl.getInt("trans-content-up")));
                }
            }


            if (urls.size() == 0) {
                print("Not found children of ^");
                if (jUrl.has("break-if-not-found") && jUrl.getBoolean("break-if-not-found") && !hasSkippedUrls) {
                    break;
                } else {
                    continue;
                }
            }

            // Handling collected URLs.
            started |= handleUrls(urls, jUrl, started, urlElements, variables, substitutes, writeHeader);

            if (!crawlAllUrls) {
                break;
            }
        }
        return started;
    }

    private boolean handleUrls(final List<URL[]> urls,
                               final JSONObject jUrl,
                               boolean started,
                               final Map<URL[], Element> urlElements,
                               final Map<String, Object> variables,
                               final List<Object> substitutes,
                               boolean writeHeader) throws XPathExpressionException, IOException, InterruptedException {
        if (jUrl.has("url")) {
            boolean first = true;
            for (URL[] urlArr : urls) {

                final String startWith = jUrl.has("start-with") ? jUrl.getString("start-with") : null;
                final String skipUntil = jUrl.has("skip-until") ? jUrl.getString("skip-until") : null;

                int i = started ? 0 : findStartIndex(urlArr, startWith, skipUntil);
                if (i == -1) {
                    continue;
                }

                if (!started && startWith != null) {
                    started = true;
                }

                final URL[] urlSubarray = new URL[urlArr.length - i];
                System.arraycopy(urlArr, i, urlSubarray, 0, urlSubarray.length);

                final Map<String, Object> tmpVariables;
                if (jUrl.has("trans-content")) {
                    tmpVariables = new HashMap<>(variables);
                    tmpVariables.putAll(crawlTransContent(urlElements.get(urlArr), jUrl.getJSONObject("trans-content"), variables, substitutes));
                } else {
                    tmpVariables = variables;
                }

                started |= crawlRecursive(urlSubarray, jUrl.getJSONObject("url"), first && writeHeader, started, tmpVariables, substitutes, true);

                if (first) {
                    first = false;
                }
            }

        } else {
            for (final URL[] urlArr : urls) {
                final Map<String, Object> tmpVariables;
                if (jUrl.has("trans-content")) {
                    tmpVariables = new HashMap<>(variables);
                    tmpVariables.putAll(crawlTransContent(urlElements.get(urlArr), jUrl.getJSONObject("trans-content"), variables, substitutes));
                } else {
                    tmpVariables = variables;
                }
                started |= crawlContents(Arrays.asList(urlArr), jUrl, writeHeader, started, tmpVariables, substitutes);
            }
        }

        return started;
    }

    private int findStartIndex(final URL[] urlArr, final String startWith, final String skipUntil) {
        int i;
        boolean started = false;
        for (i = 0; i < urlArr.length; i++) {
            final URL url = urlArr[i];

            if (startWith != null) {
                if (startWith.equals(url.toString())) {
                    started = true;
                } else {
                    print("Ignored (by start-width): " + url);
                    continue;
                }
            }

            if (!started && skipUntil != null && !skipUntil.equals(url.toString())) {
                print("Ignored (by skip-until): " + url);
                continue;
            }

            break;
        }

        if (i < urlArr.length) {
            return i;
        }

        return -1;
    }

    private String overrideString(final String rawUrl, final Map<String, Object> variables, int index, final Object overrideUrl) {
        final String tmpUrl;
        if (overrideUrl instanceof String) {
            tmpUrl = Util.format((String) overrideUrl, variables, Collections.singletonList(index));
        } else {
            final JSONObject overrideUrlJSON = (JSONObject) overrideUrl;
            StringBuilder tmpRawUrl = new StringBuilder(rawUrl);
            if (overrideUrlJSON.has("regex")) {
                final Pattern p = Pattern.compile(overrideUrlJSON.getString("regex"), Pattern.DOTALL);
                final Matcher m = p.matcher(rawUrl);
                if (m.matches()) {
                    String val;
                    try {
                        val = m.group("value");
                    } catch (IllegalArgumentException ignored) {
                        val = m.group(1);
                    }
                    tmpRawUrl.replace(0, tmpRawUrl.length(), val);
                }
            }
            if (overrideUrlJSON.has("append")) {
                final String separator = rawUrl.contains("?") ? "&" : "?";
                tmpRawUrl.append(separator);
                tmpRawUrl.append(overrideUrlJSON.getString("append"));
            }
            if (overrideUrlJSON.has("remove")) {
                final String str4remove = overrideUrlJSON.getString("remove");
                final int indexOfStr4remove = tmpRawUrl.indexOf(str4remove);
                if (indexOfStr4remove >= 0) {
                    tmpRawUrl.delete(indexOfStr4remove, indexOfStr4remove + str4remove.length());
                }
            }
            if (overrideUrlJSON.has("replace")) {
                final JSONArray replace = overrideUrlJSON.getJSONArray("replace");
                final String replaceOf = (String) replace.get(0);
                final String replaceTo = (String) replace.get(1);
                final int replaceIndex = tmpRawUrl.indexOf(replaceOf);
                if (replaceIndex >= 0) {
                    tmpRawUrl.replace(replaceIndex, replaceIndex + replaceOf.length(), replaceTo);
                }
            }
            if (overrideUrlJSON.has("prepend")) {
                tmpRawUrl.insert(0, overrideUrlJSON.getString("prepend"));
            }
            if (overrideUrlJSON.has("urldecode")) {
                try {
                    String decodedUrl = URLDecoder.decode(tmpRawUrl.toString(), "UTF-8");
                    String tmpDecodedUrl = URLDecoder.decode(decodedUrl, "UTF-8");

                    while (!decodedUrl.equals(tmpDecodedUrl)) {
                        decodedUrl = tmpDecodedUrl;
                        tmpDecodedUrl = URLDecoder.decode(decodedUrl, "UTF-8");
                    }

                    tmpRawUrl.replace(0, tmpRawUrl.length(), decodedUrl);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            tmpUrl = Util.format(tmpRawUrl.toString(),
                    variables,
                    Collections.singletonList(index));
        }
        return tmpUrl;
    }

    private List<URL> urlToRange(final String rawUrl,
                                 final URL sourceUrl,
                                 final JSONObject jUrl,
                                 final Map<String, Object> variables) throws MalformedURLException {
        final ArrayList<URL> urlArr = new ArrayList<>();
        final int startIndex = jUrl.getInt("start-index");
        final int endIndex = jUrl.getInt("end-index");
        final Object overrideUrl = jUrl.get("override-url");
        variables.put("url", rawUrl);
        for (int index = startIndex; index <= endIndex; index++) {
            final String tmpUrl = overrideString(rawUrl, variables, index, overrideUrl);
            urlArr.add(Util.getURL(sourceUrl, tmpUrl));
        }
        return urlArr;
    }

    private boolean isValidLink(final String url, final JSONArray condition) {
        if (condition.getString(0).equals("regex")) {
            final Pattern p = Pattern.compile(condition.getString(1));
            final Matcher m = p.matcher(url);
            if (condition.length() == 2) {
                return m.matches();
            }
            final String additional = condition.getString(2);
            if (additional.equals("not")) {
                return !m.matches();
            } else {
                throw new IllegalArgumentException("Illegal link additional condition");
            }
        } else {
            throw new IllegalArgumentException("Illegal link condition");
        }
    }

    public boolean crawlContents(List<URL> urls,
                                 JSONObject config,
                                 boolean writeHeader,
                                 boolean started,
                                 final Map<String, Object> variables,
                                 final List<Object> substitutes) throws InterruptedException, IOException, XPathExpressionException, JSONException {

        final JSONObject jContent;
        if (config.has("content")) {
            jContent = config.getJSONObject("content");
        } else {
            final String contentContainer = new String(Files.readAllBytes(FileSystems.getDefault().getPath(config.getString("include-content"))));
            final JSONObject jContentContainer = new JSONObject(contentContainer);
            jContent = jContentContainer.getJSONObject("content");
            config.put("content-order", jContentContainer.getJSONArray("content-order"));
        }

        final String startWith = config.has("start-with") ? config.getString("start-with") : null;
        boolean first = true;
        int delayMillis = config.has("delay") ? config.getInt("delay") : delay;
        final Collection<String> fields = config.has("content-order") ? Util.toStringArray(config.getJSONArray("content-order")) : Util.collect(jContent.keys());
        if (writeHeader && !started) { // if is continuation (started is true) then printing header is not required
            var writableFields = fields.stream().filter(field -> !field.startsWith("$")).collect(Collectors.toList());
            writer.writeHeader(writableFields);
        }

        for (final URL url : urls) {
            if (url.getPath().startsWith("/secure/download-partner-model")) {
                continue;
            }
            if (startWith != null && !started) {
                if (startWith.equals(url.toString())) {
                    started = true;
                } else {
                    print("Ignored: " + url);
                    continue;
                }
            }
            Document doc = null;
            final URL correctedUrl = loadFromDump ? Util.changeProtocolToFileIfNotExists(url, dumpRoot) : url;

            if (correctedUrl.getProtocol().equals("file") && this.isOnlyDumpRequired) {
                continue;
            }

            int waitingNo = 0;
            boolean ok = true;
            while (true) {
                try {
                    doc = XML.loadDocument(correctedUrl, sourceEncoding, getDumpRootIfNeedMakeDump());
                    break;
                } catch (IOException ex) {
                    print("Error: " + ex.getMessage() + ". URL: " + correctedUrl);
                    Thread.sleep(WAITING_MILLIS);
                    waitingNo++;
                    if (waitingNo >= WAITING_MAX_NUMBER) {
                        ok = false;
                        break;
                    }
                    waitingNo++;
                } catch (StringIndexOutOfBoundsException ignored) { // bug
                    print("Error: " + correctedUrl);
                    Logger.getGlobal().warning("Error: " + correctedUrl);
                    waitingNo++;
                    if (waitingNo >= WAITING_MAX_NUMBER) {
                        ok = false;
                        break;
                    }
                } catch (RuntimeException e) {
                    print("Error: " + e.getMessage());
                    ok = false;
                    break;
                }
            }
            if (!ok) {
                continue;
            }
            final long startMillis = System.currentTimeMillis();
            variables.put("url", url.toString());
            final int n = crawlDocument(doc, jContent, fields, variables, substitutes);
            print("Crawled: " + correctedUrl + ", millis: " + (System.currentTimeMillis() - startMillis) + ", lines: " + n);

            if (first) {
                first = false;
            }
            if (delayMillis > 0 && !correctedUrl.getProtocol().equals("file")) {
                Thread.sleep(delayMillis);
            }
        }
        return started;
    }

    private Map<String, Object> getVarsFrom(final ArrayList<Pair<String, List<String>>> content, final Map<String, Object> content2) {
        final Map<String, Object> vars = new HashMap<>();

        for (final Pair<String, List<String>> var : content) {
            if (var.getValue().size() > 0) {
                final String val = var.getValue().get(0);
                vars.put(var.getKey(), val);
            }
        }

        for (final Map.Entry<String, Object> var : content2.entrySet()) {
            String val;
            if (var.getValue() instanceof List) {
                val = ((List)var.getValue()).get(0).toString();
            } else {
                val = var.getValue().toString();
            }
            vars.put(var.getKey(), val);
        }

        return vars;
    }

    private void crawlByArray(final List<String> values,
                              final Node doc,
                              final JSONArray arr,
                              final Map<String, Object> variables,
                              final List<Object> substitutes,
                              final ArrayList<Pair<String, List<String>>> content,
                              final Map<String, Object> content2) throws XPathExpressionException {

        switch (arr.getString(0)) {
            case "var":
                final Object varValue = variables.get(arr.getString(1));
                if (varValue instanceof Iterable) {
                    for (Object varValueItem : (Iterable) varValue) {
                        values.add((String) varValueItem);
                    }
                } else {
                    values.add((String) varValue);
                }
                break;
            case "depth":
                crawlNodeDepth(values, doc, arr.getString(1));
                break;
            case "const":
                values.add(Util.format(arr.getString(1), variables, substitutes));
                break;
            case "html":
                crawlNodeHtml(values, doc, arr.getString(1));
            case "text":
                crawlNode(values, doc, arr.getString(1), true);
                break;
            case "test":
                crawlNode(values, doc, arr.getString(1), true);
                break;
            case "prepend":
                final List<String> prepValues = new ArrayList<>();
                if (arr.getString(2).startsWith("{")) {
                    final Map<String, Object> vars = new HashMap<>(variables);
                    vars.putAll(getVarsFrom(content, content2));
                    final String formattedVal = Util.format(arr.getString(2), vars, substitutes);
                    prepValues.add(formattedVal);
                } else {
                    crawlNode(prepValues, doc, arr.getString(2), true);
                }
                for (int i = 0; i < prepValues.size(); i++) {
                    final String value = prepValues.get(i);
                    prepValues.set(i, arr.getString(1) + value);
                }
                values.addAll(prepValues);
                break;
            case "default":
                final int valuesCount = values.size();
                crawlNode(values, doc, arr.getString(2), true);
                if (valuesCount == values.size()) {
                    if (arr.getString(1).startsWith("{")) {
                        final Map<String, Object> vars = new HashMap<>(variables);
                        vars.putAll(getVarsFrom(content, content2));
                        final String formattedVal = Util.format(arr.getString(1), vars, substitutes);
                        values.add(formattedVal);
                    } else {
                        values.add(arr.getString(1));
                    }
                }
                break;
            case "replace":
                if (arr.getString(3).startsWith("{")) {
                    final Map<String, Object> vars = new HashMap<>(variables);
                    vars.putAll(getVarsFrom(content, content2));
                    final String formattedVal = Util.format(arr.getString(3), vars, substitutes);
                    values.add(formattedVal.replaceAll(arr.getString(1), arr.getString(2)));
                } else {
                    crawlNode(values, doc, arr.getString(3), true);
                }
                for (int i = 0; i < values.size(); i++) {
                    final String value = values.get(i);
                    values.set(i, value.replaceAll(arr.getString(1), arr.getString(2)));
                }
                break;
            case "regex":
                crawlNode(values, doc, arr.getString(2), false, arr.length() >= 4 ? arr.getString(3) : "");
                for (int i = 0; i < values.size(); i++) {
                    final Pattern p = Pattern.compile(arr.getString(1), Pattern.DOTALL);
                    final Matcher m = p.matcher(values.get(i).trim());
                    if (m.matches()) {
                        String val;
                        try {
                            val = m.group("value");
                        } catch (IllegalArgumentException ignored) {
                            val = m.group(1);
                        }
                        values.set(i, val);
                    }
                }
                break;
            default:
                throw new RuntimeException("Unknown content field type: " + arr.getString(0));
        }
    }

    /**
     *
     * @param doc
     * @param jContent
     * @param fieldNames
     * @throws XPathExpressionException
     * @throws IOException
     */
    public int crawlDocument(final Document doc,
                             final JSONObject jContent,
                             final Collection<String> fieldNames,
                             final Map<String, Object> variables,
                             final List<Object> substitutes) throws XPathExpressionException, IOException, JSONException {
        final ArrayList<Pair<String, List<String>>> content = new ArrayList<>();

        System.out.println("-------------------------------------------------------------------");
        final long startMillis = System.currentTimeMillis();
        for (String fieldName : fieldNames) {
            final Object obj = jContent.get(fieldName);
            final List<String> values = new ArrayList<>();
            if (obj instanceof String) {
                crawlNode(values, doc, (String) obj, false);
            } else if (obj instanceof JSONArray) {
                crawlByArray(values, doc, (JSONArray) obj, variables, substitutes, content, new HashMap<>());
            } else if (obj instanceof JSONObject) {
                crawlByJSONObject(values, doc, (JSONObject) obj);
            }
            for (int i = 0; i < values.size(); i++) {
                if (values.get(i) != null) {
                    values.set(i, values.get(i).trim());
                }
            }

            content.add(Pair.create(fieldName, values));
        }

        final long startWritingMillis = System.currentTimeMillis();
        System.out.println("------ CRAWLING: " + (startWritingMillis - startMillis) + "ms");

        var writableContent = content.stream().filter(x -> !x.first.startsWith("$")).collect(Collectors.toList());
        final int ret = writer.write(writableContent);

        System.out.println("------ SAVING: " + (System.currentTimeMillis() - startWritingMillis) + "ms");

        final long finishMillis = System.currentTimeMillis();
        System.out.println("------ TOTAL: " + (finishMillis - this.lastWriteMillis) + "ms");
        this.lastWriteMillis = finishMillis;
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        return ret;
    }

    protected void crawlNode(final List<String> values,
                             final Node node,
                             final String path,
                             final boolean textOnly) throws XPathExpressionException {
        crawlNode(values, node, path, textOnly, "");
    }

    protected void crawlNode(final List<String> values,
                             final Node node,
                             final String path,
                             final boolean textOnly,
                             final String separator) throws XPathExpressionException {
        final XPath xPath = xPathFactory.newXPath();
        final NodeList nodes = (NodeList) xPath.evaluate(path, node, XPathConstants.NODESET);
        final List<String> textOfNodes = XML.getNodeListText(nodes, textOnly, separator);
        values.addAll(textOfNodes);
    }

    protected void crawlNodeHtml(final List<String> values,
                                 final Node node,
                                 final String path) throws XPathExpressionException {
        final XPath xPath = xPathFactory.newXPath();
        final NodeList nodes = (NodeList) xPath.evaluate(path, node, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            values.add(XML.toString(nodes.item(i)));
        }
    }

    protected void crawlNodeDepth(final List<String> values,
                                  final Node node,
                                  final String path) throws XPathExpressionException {
        final XPath xPath = xPathFactory.newXPath();
        final NodeList nodes = (NodeList) xPath.evaluate(path, node, XPathConstants.NODESET);
        final List<String> depths = XML.getNodeListDepth(nodes).stream().map(Object::toString).collect(Collectors.toList());
        values.addAll(depths);
    }

    protected void crawlByJSONObject(final List<String> values, final Node node, final JSONObject obj) throws XPathExpressionException, JSONException {
        final XPath xPath = xPathFactory.newXPath();
        final String path = obj.getString("path");
        final NodeList nodes = (NodeList) xPath.evaluate(path, node, XPathConstants.NODESET);
        if (obj.has("sequence")) {
            crawlBySequence(values, obj.getJSONArray("sequence"), nodes);
        } else {
            throw new RuntimeException("Illegal content item");
        }
    }

    /**
     *
     * @param values
     * @param sequence
     * @param nodeSequence
     * @throws XPathExpressionException
     * @throws JSONException
     */
    private void crawlBySequence(List<String> values, JSONArray sequence, NodeList nodeSequence) throws XPathExpressionException, JSONException {
        int nodeIndex = 0;
        boolean discontinuity = false;
        final Map<String, String> variables = new HashMap<>();
        while (!discontinuity && nodeIndex < nodeSequence.getLength()) {
            for (int i = 0; i < sequence.length(); i++) {
                final JSONObject obj = sequence.getJSONObject(i);
                final String path = obj.getString("path");
                final int repetitions = obj.has("repetitions") ? obj.getInt("repetitions") : -1;
                int n = 0;
                while (repetitions == -1 || n < repetitions) {
                    final Node node = nodeSequence.item(nodeIndex);
                    final XPath xPath = xPathFactory.newXPath();
                    final NodeList subnodes;
                    try {
                        subnodes = (NodeList) xPath.evaluate(path, node, XPathConstants.NODESET);
                    } catch (XPathExpressionException ex) {
                        if (repetitions != -1) {
                            discontinuity = true;
                        }
                        break;
                    }
                    if (subnodes.getLength() == 0) {
                        if (repetitions != -1) {
                            discontinuity = true;
                        }
                        break;
                    }
                    final int len = obj.has("first-only") && obj.getBoolean("first-only") ? 1 : subnodes.getLength();
                    for (int k = 0; k < len; k++) {
                        final Node x = subnodes.item(k);
                        if (obj.has("save-to-var")) {
                            variables.put(obj.getString("save-to-var"), XML.getNodeText(x));
                        } else if (obj.has("save-to-db-format")) {
                            values.add(Util.format(obj.getString("save-to-db-format"), variables, XML.getNodeText(x)));
                        }
                    }
                    nodeIndex++;
                    n++;
                }
                if (discontinuity) {
                    break;
                }
            }
        }

        if (discontinuity) {
            print("Discontinuity");
        }
    }

    private void print(final String s) {
        final PrintStream tmp = System.out;
        tmp.println(s);
    }
    /**
     *
     * @param config Configuration from config-file.
     * @param oUrl URL for crawling. It can be string or JSON object.
     * @param variables Key->value pairs for inserting to URL. Used in Util.format method.
     * @param substitutes Array of values for inserting to URL. Used in Util.format method.
     * @param writeHeader Call writeHeader method first. It useful for storing data to CSV file.
     * @param started
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws XPathExpressionException
     * @throws JSONException
     */
    protected boolean crawlContentsOrRecursive(final JSONObject config, final Object oUrl,
                                               final Map<String, Object> variables, final List<Object> substitutes,
                                               boolean writeHeader, boolean started) throws IOException, InterruptedException, XPathExpressionException, JSONException {
        if (oUrl instanceof String) {
            final String url = Util.format((String) oUrl, variables, substitutes);
            return crawlContents(Collections.singletonList(new URL(url)), config, writeHeader, started, variables, substitutes);
        } else {
            final JSONObject jUrl = (JSONObject) oUrl;
            final Object sourceURL = jUrl.get("source-url");
            final ArrayList<URL> urls = new ArrayList<>();
            if (sourceURL instanceof String) {
                final String url = Util.format(jUrl.getString("source-url"), variables, substitutes);
                urls.add(new URL(url));
            } else {
                final JSONArray arr = jUrl.getJSONArray("source-url");
                for (int i = 0; i < arr.length(); i++) {
                    final String url = Util.format(arr.getString(i), variables, substitutes);
                    urls.add(new URL(url));
                }
            }
            final boolean crawlAll = jUrl.has("crawl-all") && jUrl.getBoolean("crawl-all");
            return crawlRecursive(urls.toArray(new URL[urls.size()]), jUrl, writeHeader, started, variables, substitutes, crawlAll);
        }
    }

    /**
     * Enter point.
     * <h2>Configuration file description</h2>
     * Now supported 3 modes:
     * 1) RANGE OF PAGES if options "start-index" & "end-index" is specified.
     * 2) LIST OF URS if option "url" is specified and it is array.
     * 3) LIST OF FILES
     * 4) RECURSIVE
     */
    public void crawl(final JSONObject config) throws InterruptedException, XPathExpressionException, IOException, JSONException {
        boolean started = false;
        final Object oUrl = config.has("url") ? config.get("url") : null;
        if (config.has("start-index") && !config.has("end-index")) {
            throw new RuntimeException("Missing end-index field");
        }
        if (!config.has("start-index") && config.has("end-index")) {
            throw new RuntimeException("Missing start-index field");
        }
        if (config.has("start-index") && config.has("end-index")) {
            final int step = config.has("step") ? config.getInt("step") : 1;
            boolean first = true;
            for (int i = config.getInt("start-index"); i <= config.getInt("end-index"); i += step) {
                started = crawlContentsOrRecursive(config, oUrl, new HashMap<>(), Collections.singletonList(i), first, started);
                first = false;
            }
            // array of URLs
        } else if (oUrl != null && oUrl instanceof JSONArray) {
            final ArrayList<URL> urlList = new ArrayList<>();
            for (final String sUrl : Util.toStringArray((JSONArray) oUrl)) {
                urlList.add(new URL(sUrl));
            }
            crawlContents(urlList, config, false, started, new HashMap<>(), Collections.emptyList());
            // list of files (for loading data from files)
        } else if (config.has("list-file")) {
            final BufferedReader reader = new BufferedReader(new FileReader(config.getJSONObject("list-file").getString("file-naturalKey")));
            final Map<String, Object> var = new HashMap<>();
            boolean first = true;
            String line = reader.readLine();
            while (line != null) {
                var.put(config.getJSONObject("list-file").getString("var"), line.trim());
                started = crawlContentsOrRecursive(config, oUrl, var, Collections.emptyList(), first, started);
                first = false;
                line = reader.readLine();
            }
            // recursive
        } else {
            crawlContentsOrRecursive(config, oUrl, new HashMap<>(), Collections.emptyList(), true, started);
        }
    }

}
