/**
 * Created: 20.02.15 14:29
 */
package com.fiftin;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */
public class XML {
    private static class ProxyInfo {
        public final String host;
        public final int port;
        public final Proxy.Type type;
        public int requests;
        public int errors;

        private ProxyInfo(String host, int port, Proxy.Type type) {
            this.host = host;
            this.port = port;
            this.type = type;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(host).append(port).append(type).toHashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ProxyInfo)) {
                return false;
            }
            final ProxyInfo other = (ProxyInfo) obj;
            return this.host.equals(other.host)
                    && this.port == other.port
                    && this.type == other.type;
        }
    }

    private static List<ProxyInfo> PROXIES = new ArrayList<>();
    private static List<ProxyInfo> RELIABLE_PROXIES = new ArrayList<>();

    private static Random RND = new Random();

    private XML() {
    }

    public static void registerProxy(final String host, final int port, final Proxy.Type type) {
        final ProxyInfo newProxy = new ProxyInfo(host, port, type);
        if (PROXIES.indexOf(newProxy) >= 0) {
            return;
        }
        PROXIES.add(newProxy);
    }

    public static String getNodeText(final Node node) {
        return getNodeText(node, false);
    }


    public static String getNodeText(final Node node, final boolean textOnly) {
        return getNodeText(node, textOnly, "");
    }

    public static int getNodeDepth(final Node node) {
        int ret = 0;
        Node parent = node.getParentNode();
        while (parent != null) {
            ret++;
            parent = parent.getParentNode();
        }
        return ret;
    }
    /**
     * Getting text from node (recursively if textOnly is true).
     * @param textOnly Returns only text contained in the node. Or returns text of inner nodes recursively.
     * @return Inner text of node.
     */
    public static String getNodeText(final Node node, final boolean textOnly, final String separator) {
        if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.ATTRIBUTE_NODE) {
            return node.getNodeValue();
        }
        final int n = node.getChildNodes().getLength();
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < n; i++) {
            final Node child = node.getChildNodes().item(i);
            final int type = child.getNodeType();
            switch (type) {
                case Node.TEXT_NODE:
                    if (result.length() > 0) {
                        result.append(separator);
                    }
                    result.append(child.getNodeValue());
                    break;
                case Node.ELEMENT_NODE:
                    if (textOnly) {
                        break;
                    }
                    final Element childElement = (Element) child;
                    if (childElement.getTagName().equals("br")) {
                        result.append("\n");
                    } else {
                        if (result.length() > 0) {
                            result.append(separator);
                        }
                        result.append(getNodeText(child, false, separator));
                    }
                    break;
                default:
                    break;
            }
        }
        return result.toString();
    }

    /**
     * Getting list of strings from received nodes. Each element of list is inner text of appropriate node.
     * @param nodes List of nodes.
     * @param textOnly
     * @return List of strings from received nodes. Each element of list is inner text of appropriate node.
     */
    public static List<String> getNodeListText(final NodeList nodes, boolean textOnly, final String separator) {
        final ArrayList<String> ret = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            ret.add(getNodeText(nodes.item(i), textOnly, separator));
        }
        return ret;
    }

    public static List<Integer> getNodeListDepth(final NodeList nodes) {
        final ArrayList<Integer> ret = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            ret.add(getNodeDepth(nodes.item(i)));
        }
        return ret;
    }

    public static Document loadDocument(final URL url) throws IOException {
        return loadDocument(url, "utf-8", null);
    }


    private static ProxyInfo getRandomProxy() {
        if (PROXIES.size() == 0) {
            return null;
        }
//        return PROXIES.get(RND.nextInt(PROXIES.size()));

        ProxyInfo tmpProxyInfo = PROXIES.get(RND.nextInt(PROXIES.size()));
        boolean found = false;

        // 5 attempts to find valid proxy
        int i = 0;
        while (i <= 4) {
            final float factor = 0.05f * i;
            if (tmpProxyInfo.requests <= 3 || ((float)tmpProxyInfo.errors / (float)tmpProxyInfo.requests) < 0.6 + factor) {
                found = true;
                break;
            }
            tmpProxyInfo = PROXIES.get(RND.nextInt(PROXIES.size()));
            i++;
        }
        if (found) {
            return tmpProxyInfo;
        } else if (RELIABLE_PROXIES.size() > 0) {
            return RELIABLE_PROXIES.get(RND.nextInt(RELIABLE_PROXIES.size()));
        } else {
            return null;
        }
    }

    public static String toString(final Node node) {
        final Tidy tidy = getTidy("utf-8");
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        tidy.pprint(node, stream);
        final String ret;
        try {
            ret = stream.toString("utf-8");
            return ret;
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    private static Tidy getTidy(final String encoding) {
        final Tidy tidy = new Tidy();
        tidy.setInputEncoding(encoding);
        tidy.setXHTML(true);
        tidy.setShowErrors(1000);
        tidy.setShowWarnings(false);
        tidy.setQuiet(true);


        return tidy;
    }

    /**
     * Load document by it URL. Store loaded document to local directory if dumpRoot isn't null and URL protocol isn't file.
     */
    public static Document loadDocument(final URL url, final String encoding, final String dumpRoot) throws IOException {
        final InputStream input;
        if (url.getProtocol().equals("file")) {
            input = new FileInputStream(url.getPath());
        } else {
            final ProxyInfo proxyInfo = getRandomProxy();
            final OkHttpClient client;

            if (proxyInfo != null) {
                final Proxy proxy = new Proxy(proxyInfo.type, new InetSocketAddress(proxyInfo.host, proxyInfo.port));
                client = new OkHttpClient.Builder().proxy(proxy).build();
            } else {
                client = new OkHttpClient();
            }

            final Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36")
                    .build();

            final Response response;
            try {
                if (proxyInfo != null) {
                    proxyInfo.requests++;
                }
                response = client.newCall(request).execute();
                if (!RELIABLE_PROXIES.contains(proxyInfo)) {
                    RELIABLE_PROXIES.add(proxyInfo);
                }
            } catch (IOException | RuntimeException e) {
                if (proxyInfo != null) {
                    proxyInfo.errors++;
                    RELIABLE_PROXIES.remove(proxyInfo);
                }
                throw e;
            }

            final InputStream inputStream = response.body().byteStream();

            try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                Util.copyStream(inputStream, outputStream);
                input = new ByteArrayInputStream(outputStream.toByteArray());
                if (dumpRoot != null) {
                    // dump

                    final String dumpDirName = Util.changeProtocolToFile(url, dumpRoot).getFile();

                    final File dumpFileDir = new File(FilenameUtils.getFullPathNoEndSeparator(dumpDirName));
                    if (!dumpFileDir.exists()) {
                        //noinspection ResultOfMethodCallIgnored
                        dumpFileDir.mkdirs();
                    }

                    final String dumpFileName = dumpDirName.endsWith(".html") ? dumpDirName : dumpDirName + ".html";
                    final File dumpFile = new File(dumpFileName);
                    if (!dumpFile.exists()) {
                        try (final FileOutputStream dumpOutput = new FileOutputStream(dumpFileName)) {
                            outputStream.writeTo(dumpOutput);
                        }
                    }
                }
            }
        }

        final Tidy tidy = getTidy(encoding);
        //noinspection UnnecessaryLocalVariable

        StringWriter writer = new StringWriter();
        IOUtils.copy(input, writer, encoding);
        String content = writer.toString()
                .replaceAll("<script\\b[^<]*(?:(?!</script>)<[^<]*)*</script>", "")
                .replaceAll("<section>", "<div>")
                .replaceAll("<section\\s", "<div ")
                .replaceAll("</section>", "</div>")
                .replaceAll("<header>", "<div>")
                .replaceAll("<header\\s", "<div ")
                .replaceAll("</header>", "</div>")
                .replaceAll("<main>", "<div>")
                .replaceAll("<main\\s", "<div ")
                .replaceAll("</main>", "</div>")
                .replaceAll("<article>", "<div>")
                .replaceAll("<article\\s", "<div ")
                .replaceAll("</article>", "</div>")
                .replaceAll("<time>", "<span>")
                .replaceAll("<time\\s", "<span ")
                .replaceAll("</time>", "</span>")
                .replaceAll("<footer>", "<div>")
                .replaceAll("<footer\\s", "<div ")
                .replaceAll("</footer>", "</div>")
                .replaceAll("<nav>", "<div>")
                .replaceAll("<nav\\s", "<div ")
                .replaceAll("</nav>", "</div>");

        final StringReader reader = new StringReader(content);
        final Document doc = tidy.parseDOM(reader, null);
        return doc;
    }

    public static Element getParent(final Element el, final int hierarchyUp) {
        Element ret = el;
        for (int i = 0; i < hierarchyUp; i++) {
            ret = (Element) ret.getParentNode();
            if (ret == null) {
                throw new RuntimeException("Out of hierarchy");
            }
        }
        return ret;
    }
}
