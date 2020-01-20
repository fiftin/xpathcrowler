/**
 * Created: 20.02.15 14:27
 */
package com.fiftin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */
public class Util {

    private static final int BUF_SIZE = 0x100000; // 400K
    private static final int TEXT_STATE = 0;
    private static final int VAR_STATE = 1;
    private static final int VAR_TYPE_STATE = 2;
    public static final Pattern REPLACE_PATTERN = Pattern.compile("^replace-word:(.*)$");
    public static final Pattern REMOVE_PATTERN = Pattern.compile("^remove-word:(.*)$");

    private Util() { }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
    private static String getMD5(String s) {
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        md.update(s.getBytes());
        final byte[] digest = md.digest();
        return Util.bytesToHex(digest);
    }

    public static List collect(Iterator iterator) {
        ArrayList ret = new ArrayList();
        while (iterator.hasNext()) {
            //noinspection unchecked
            ret.add(iterator.next());
        }
        return ret;
    }
    public static String escapeSQLString(final String str) {
        if (str == null) {
            return null;
        }
        final String ret = str.replaceAll("'+", "''");
        return ret;
    }

    public static Map<String, List<String>> mapSlice(final Map<String, List<String>> map, final int index, final String scope) {
        final HashMap<String, List<String>> ret = new HashMap<>();
        for (final Map.Entry<String, List<String>> pair : map.entrySet()) {
            if (pair.getValue().size() < index) {
                continue;
            }
            if (scope != null && !Objects.equals(scope, "") && !pair.getKey().startsWith(scope + ".")) {
                continue;
            }
            final String key;
            if (scope != null && !Objects.equals(scope, "")) {
                key = pair.getKey().substring(pair.getKey().indexOf(".") + 1);
            } else {
                key = pair.getKey();
            }
            ret.put(key, Collections.singletonList(pair.getValue().get(index)));
        }
        if (ret.isEmpty()) {
            throw new RuntimeException("Empty slice");
        }
        return ret;
    }

    public static Map<String, List<String>> pairListToMap(final List<Pair<String, List<String>>> pairs) {
        final HashMap<String, List<String>> ret = new HashMap<>();
        for (final Pair<String, List<String>> pair : pairs) {
            ret.put(pair.getKey(), pair.getValue());
        }
        return ret;
    }

    public static String getStringOrEmpty(final JSONObject obj, final String key) {
        if (obj.has(key)) {
            return obj.getString(key);
        }
        return "";
    }

    public static Set<String> getStringSet(final JSONObject obj, final String key) {
        final TreeSet<String> ret = new TreeSet<>();
        if (obj.has(key)) {
            final Object arr = obj.get(key);
            if (arr instanceof List) {
                ret.addAll((List<String>) arr);
            } else {
                final JSONArray jArr = obj.getJSONArray(key);
                for (int i = 0; i < jArr.length(); i++) {
                    ret.add(jArr.getString(i));
                }
            }
        }
        return ret;
    }

    public static Map<String, String> getStringMap(final JSONObject obj, final String key) {
        final HashMap<String, String> ret = new HashMap<>();
        if (obj.has(key)) {
            for (final String k : obj.getJSONObject(key).keySet()) {
                ret.put(k, obj.getJSONObject(key).getString(k));
            }
        }
        return ret;
    }

    public static String getStringOrNull(final JSONObject obj, final String key) {
        if (obj.has(key)) {
            return obj.getString(key);
        }
        return null;
    }

    public static String firstOrEmpty(final List<String> list) {
        if (list.size() <= 0) {
            return "";
        }
        return list.get(0);
    }

    /**
     * Replace protocol in URL to file if file exists.
     */
    public static URL changeProtocolToFileIfNotExists(final URL url, final String rootPath) throws MalformedURLException {
        final URL fileURL = changeProtocolToFile(url, rootPath);
        final String fileName = fileURL.getFile();
        final File file = new File(fileName);
        if (!file.exists()) {
            return url;
        }
        return fileURL;
    }

    /**
     * Replace protocol in URL to file.
     */
    public static URL changeProtocolToFile(final URL url, final String rootPath) throws MalformedURLException {
        final String pathOrig = url.getFile().endsWith("/") ? url.getFile().substring(0, url.getFile().length() - 1) : url.getFile();
        final String path = pathOrig.replace('?', '_');
        final String pathWithExt = path.endsWith(".html") ? path : path  + ".html";

        final String filePath = rootPath + "/" + url.getHost() + pathWithExt;
        if (filePath.length() <= 255) {
            return new URL("file", "", filePath);
        } else {
            final String hash = getMD5(pathOrig);
            return new URL("file", "", rootPath + "/" + url.getHost() + "/" + hash + ".html");
        }
    }

    /**
     * Copy one stream to another.
     */
    public static long copyStream(InputStream from, OutputStream to)
            throws IOException {
        byte[] buf = new byte[BUF_SIZE];
        long total = 0;
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }

    /**
     * Convert JSON array to string list.
     */
    public static List<String> toStringArray(final JSONArray obj) throws JSONException {
        final ArrayList<String> ret = new ArrayList<>();
        for (int i = 0; i < obj.length(); i++) {
            ret.add(obj.getString(i));
        }
        return ret;
    }

//    public static boolean containsURL(final Collection<URL> urls, final URL url) {
//        for (final URL x : urls) {
//            if (x.toString().equals(url.toString()))
//                return true;
//        }
//        return false;
//    }

    public static boolean containsURL(final Collection<URL[]> urls, final URL url) {
        for (final URL[] x : urls) {
            for (final URL u : x) {
                if (u.toString().equals(url.toString())) {
                    return true;
                }
            }
        }
        return false;
    }


    public static URL getURL(final URL sourceUrl, final String rawUrl) throws MalformedURLException {
        if (rawUrl.startsWith("http")) {
            return new URL(rawUrl);
        }

        if (rawUrl.startsWith("/")) {
            return new URL(sourceUrl.getProtocol() + "://" + sourceUrl.getAuthority() + rawUrl);
        }

        int index = sourceUrl.toString().lastIndexOf("/");
        if (index != -1) {
            return new URL(sourceUrl.toString().substring(0, index + 1) + rawUrl);
        }

        throw new MalformedURLException();
    }

    public static String format(final Object val, final String commaSeparatedTypes) {
        final String[] types = commaSeparatedTypes.split(",");
        String ret = val.toString();
        for (final String type : types) {
            final String trimmedType = type.trim();
            switch (trimmedType) {
                case "url":
                    try {
                        ret = URLEncoder.encode(ret, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "before-hyphen":
                    final int hyphenIndex = ret.indexOf(" - ");
                    if (hyphenIndex != -1) {
                        ret = ret.substring(0, hyphenIndex);
                    }
                    break;
                case "one-space-and-order-words":
                    final String[] oneSpaceAndOrderWorlds = ret.split("\\s");
                    Arrays.sort(oneSpaceAndOrderWorlds);
                    final StringBuilder builder = new StringBuilder();
                    for (final String w : oneSpaceAndOrderWorlds) {
                        if (w.isEmpty()) {
                            continue;
                        }
                        if (builder.length() != 0) {
                            builder.append(" ");
                        }
                        builder.append(w);
                    }
                    ret = builder.toString();
                    break;
                case "trim":
                    ret = ret.trim();
                    break;
                case "max-word":
                    final String[] words = ret.split("\\s");
                    ret = "";
                    for (final String w : words) {
                        if (w.length() > ret.length()) {
                            ret = w;
                        }
                    }
                    break;
                case "lower":
                    ret = ret.toLowerCase();
                    break;
                default:
                    final Matcher m = REPLACE_PATTERN.matcher(trimmedType);
                    if (m.matches()) {
                        final String[] wordsForRemove = m.group(1).split(";");
                        for (final String w : wordsForRemove) {
                            final String[] tokens = w.split("/");
                            if (tokens.length != 2) {
                                throw new RuntimeException(w);
                            }
                            if (tokens[0].equals("&amp")) {
                                ret = ret.replace("&", tokens[1]);
                            } else {
                                ret = ret.replaceAll("\\b" + tokens[0] + "\\b", tokens[1]);
                            }
                        }
                    } else {
                        final Matcher m2 = REMOVE_PATTERN.matcher(trimmedType);
                        if (m2.matches()) {
                            final String[] wordsForRemove = m2.group(1).split(";");
                            for (final String w : wordsForRemove) {
                                if (w.equals("&amp")) {
                                    ret = ret.replace("&", "");
                                } else {
                                    ret = ret.replaceAll("\\b" + w.trim() + "\\b", "");
                                }
                            }
                        }
                    }
                    break;
            }
        }
        return ret;
    }

    public static boolean isInt(final CharSequence str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static <T> String format(final String format, final Map<String, T> variables, Object s) {
        return format2(format, variables, s);
    }

    public static <T> String format(final String format, final Map<String, T> variables, final List<Object> s) {
        return format2(format, variables, s.toArray());
    }

    private static <T> String format2(final String format, final Map<String, T> variables, final Object... s) {
        final StringBuilder ret = new StringBuilder();
        final StringBuilder var = new StringBuilder();
        Object varValue = null;
        final StringBuilder varType = new StringBuilder();
        int state = TEXT_STATE;
        for (int i = 0; i < format.length(); i++) {
            char c = format.charAt(i);
            switch (c) {
                case '{':
                    if (state == VAR_STATE) {
                        throw new RuntimeException("Illegal character: " + c);
                    }
                    if (i + 1 < format.length() && format.charAt(i + 1) == ' ') {
                        if (state == VAR_TYPE_STATE) {
                            varType.append(c);
                        } else {
                            ret.append(c);
                        }
                        break;
                    }
                    var.setLength(0);
                    state = VAR_STATE;
                    break;
                case '}':
                    if (state == VAR_STATE) {
                        final Object val;
                        if (isInt(var)) {
                            val = s[Integer.parseInt(var.toString())];
                        } else {
                            val = variables.get(var.toString());
                        }
                        ret.append(val == null ? "" : val);
                        var.setLength(0);
                        state = TEXT_STATE;
                    } else if (state == VAR_TYPE_STATE) {
                        ret.append(format(varValue, varType.toString()));
                        state = TEXT_STATE;
                    } else {
                        ret.append(c);
                    }
                    break;
                case ':':
                    if (state == VAR_STATE) {
                        if (isInt(var)) {
                            varValue = s[Integer.parseInt(var.toString())];
                        } else {
                            varValue = variables.get(var.toString());
                        }
                        var.setLength(0);
                        state = VAR_TYPE_STATE;
                    } else if (state == VAR_TYPE_STATE) {
                        varType.append(c);
                    } else {
                        ret.append(c);
                    }
                    break;
                default:
                    if (state == VAR_STATE) {
                        var.append(c);
                    } else if (state == VAR_TYPE_STATE) {
                        varType.append(c);
                    } else {
                        ret.append(c);
                    }
                    break;
            }
        }
        if (state != TEXT_STATE) {
            throw new RuntimeException("Unexpected end");
        }
        return ret.toString();
    }

    public static <T> String format(final String format, final Map<String, T> variables) {
        return format2(format, variables);
    }

    public static List<String> getKeys(final List<Pair<String, List<String>>> keys) {
        final ArrayList<String> ret = new ArrayList<>();
        for (final Pair<String, List<String>> x : keys) {
            ret.add(x.getKey());
        }
        return ret;
    }

    public static List<String> split(final String s, final String separator) {
        final String[] parts = s.split(separator);
        final ArrayList<String> ret = new ArrayList<>();
        for (final String x : parts) {
            if (x.isEmpty()) {
                continue;
            }
            ret.add(x);
        }
        return ret;
    }

    public static boolean appendNotFirst(final String s, final Writer writer, boolean first) throws IOException {
        if (!first) {
            writer.append(s);
        }
        return false;
    }

    public static final int BUFFER_SIZE = 100;

    public static String readToEnd(final InputStream in) throws IOException {
        final Reader reader = new InputStreamReader(in);
        final char[] buffer = new char[BUFFER_SIZE];
        final StringBuilder ret = new StringBuilder();
        int n;
        while ((n = reader.read(buffer)) != -1) {
            ret.append(buffer, 0, n);
        }
        return ret.toString();
    }

    public static <K, V>  Map<K, V> map(List<K> keys, List<V> values) {
        assert keys.size() == values.size();
        final Map<K, V> ret = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            ret.put(keys.get(i), values.get(i));
        }
        return ret;
    }

    public static List<Integer> range(int first, int last) {
        final ArrayList<Integer> ret = new ArrayList<>();
        for (int i = first; i <= last; i++) {
            ret.add(i);
        }
        return ret;
    }

    public static <T> List<T> flat(List<T[]> lst) {
        final ArrayList<T> ret = new ArrayList<>();
        for (final T[] arr : lst) {
            ret.addAll(Arrays.asList(arr));
        }
        return ret;
    }


    public static <K, V> void print(Map<K, V> value) {
        for (final Map.Entry<K,V> x : value.entrySet()) {
            System.out.println("" + x.getKey() + " = " + x.getValue());
        }
        System.out.println("----------------");
    }

}
