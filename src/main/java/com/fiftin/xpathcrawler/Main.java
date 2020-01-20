package com.fiftin.xpathcrawler;

import com.fiftin.CSVWriter;
import com.fiftin.MyWriter;
import com.fiftin.Util;
import com.fiftin.XML;
import com.fiftin.db.DBConfig;
import com.fiftin.db.DBWriter;
import com.fiftin.db.RequestConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

public class Main {
    private static void loadProxies(final String fileName) throws IOException {
        try (
                final InputStream fis = new FileInputStream(fileName);
                final InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
                final BufferedReader br = new BufferedReader(isr)
        ) {
            String line;
            while ((line = br.readLine()) != null) {
                final String[] parts = line.split(" ");

                XML.registerProxy(parts[0], Integer.parseInt(parts[1]), Enum.valueOf(Proxy.Type.class, parts.length < 3 ? "SOCKS" : parts[2]));
            }
        }
    }

    private static JSONObject[] getConfigs(String configFile) throws IOException {
        final String config = configFile == null ? Util.readToEnd(System.in) : new String(Files.readAllBytes(FileSystems.getDefault().getPath(configFile)));

        final List<JSONObject> ret = new ArrayList<>();

        if (config.trim().startsWith("[")) {
            final JSONArray arr = new JSONArray(config);
            for (int i = 0; i < arr.length(); i++) {
                ret.add((JSONObject) arr.get(i));
            }
        } else {
            ret.add(new JSONObject(config));
        }

        return ret.toArray(new JSONObject[ret.size()]);
    }

    private static void crawl(final JSONObject config,
                              final String output,
                              final boolean isDumpOnly,
                              final String dumpRoot,
                              final Map<String, String> fields) throws SQLException, IOException, XPathExpressionException, InterruptedException, ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException {
        final XPathFactory xPathFactory = XPathFactory.newInstance();
        final MyWriter writer;

        if (config.has("out")) {
            final JSONObject out = config.getJSONObject("out");
            if (out.has("db")) {
                final DBConfig db = DBConfig.load(out.getJSONObject("db"));
                final RequestConfig req = db.requests.get(0);
                writer = new DBWriter(req.table,
                        req.columns,
                        req.filters,
                        req.mode, DriverManager.getConnection(db.connectionString, db.user, db.password));
            } else {
                throw new RuntimeException("Unknown out type");
            }
        } else {
            switch (output) {
                case "":
                    writer = new CSVWriter(new PrintWriter(System.out));
                    break;
                default:
                    final Class writerClass = Class.forName(output);
                    final Constructor ctor = writerClass.getConstructors()[0];
                    writer = (MyWriter) ctor.newInstance(fields);
                    writer.auth();
                    break;
            }
        }

        final boolean saveDump;
        final int delay;

        saveDump = true;
        delay = 1000;

        final Crawler crawler = new Crawler(writer, xPathFactory, "utf-8", dumpRoot, saveDump, true, delay, isDumpOnly);
        crawler.crawl(config);
    }

    public static void main(String[] args) throws IOException, XPathExpressionException, InterruptedException, SQLException, ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
        boolean     isParallel = false;
        int         nParallels = 10;
        String      output = ""; // Writer class fullname. Print to stdout if empty.
        int         configIndex = -1;
        boolean     isOnlyDump = false;
        String      dir = null;
        String      dumpRoot = "D:\\xpathcrawler_dumps";
        String      proxiesFilename = null;

        final Map<String, String> fields = new HashMap<>();
        for (int i = 1; i < args.length; i++) {
            final String arg = args[i];
            if (arg.startsWith("--")) {
                final int index = arg.indexOf("=");
                final String argName = index >= 0 ? arg.substring(0, index) : arg;
                final String argValue = index >= 0 ? arg.substring(index + 1) : "";
                switch (argName) {
                    // Writer class fullname
                    case "--output":
                        output = argValue;
                        break;
                    case "--parallel":
                        isParallel = true;
                        if (!Objects.equals(argValue, "")) {
                            nParallels = Integer.parseInt(argValue);
                        }
                        break;
                    case "--index":
                        configIndex = Integer.parseInt(argValue);
                        break;
                    case "--only-dump":
                        isOnlyDump = true;
                        break;
                    case "--dump-root":
                        dumpRoot = argValue;
                        break;
                    // TODO: what reasons to use this parameter?
                    case "--dir":
                        dir = argValue;
                        break;
                    // Use proxies. Default proxies will be used if pathname isn't specified.
                    case "--proxies":
                        proxiesFilename = argValue.isEmpty() ?  "D:\\src\\xpathcrawler\\src\\test\\resources\\com\\fiftin\\xpathcrawler\\proxies.txt" : argValue;
                        break;
                }
            } else if (arg.startsWith("-F")) {
                fields.put(arg.substring(2), args[i + 1]);
                i++;
            }
        }

        final JSONObject[] configs = getConfigs(args.length == 0 ? null : args[0]);

        System.out.println("######### Crawler configuration #########");

        System.out.println(    "Output:   " + (output.equals("") ? "stdout" : output));

        if (configIndex >= 0) {
            System.out.println("Configs:  #" + configIndex + " only");
        } else {
            System.out.println("Configs:  " + configs.length);
        }

        if (isParallel) {
            System.out.println("Run mode: Parallel");
        } else {
            System.out.println("Run mode: Sequential");
        }

        System.out.println("#########################################");
        System.out.println();

        if (proxiesFilename != null && !proxiesFilename.isEmpty()) {
            loadProxies(proxiesFilename);
        }

        if (dir != null) {
            final JSONObject config = configs[0];
            final File folder = new File(dir);
            final File[] files = folder.listFiles();
            if (files == null) {
                System.out.println("No files in " + dir);
                return;
            }
            if (isParallel) {
                final int filesPerThread = files.length / nParallels;
                final int filesPerLastThread = filesPerThread + files.length - filesPerThread * nParallels;
                for (int i = 0; i < nParallels; i++) {
                    final String fixedOutput = output;
                    final boolean fixedIsDumpOnly = isOnlyDump;
                    final int startIndex = i * filesPerThread;
                    final int n = i == nParallels - 1 ? filesPerLastThread : filesPerThread;
                    final String fixedDumpRoot = dumpRoot;
                    new Thread(() -> {
                        try {
                            for (int k = startIndex; k < startIndex + n; k++) {
                                if (files[k].isDirectory()) {
                                    final File[] subFiles = files[k].listFiles();
                                    if (subFiles != null && subFiles.length > 0) {
                                        config.put("url", subFiles[0].toURI().toString());
                                        crawl(config, fixedOutput, fixedIsDumpOnly, fixedDumpRoot, fields);
                                    }
                                } else if (files[k].isFile()) {
                                    config.put("url", files[k].toURI().toString());
                                    crawl(config, fixedOutput, fixedIsDumpOnly, fixedDumpRoot, fields);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            } else {
                for (final File file : files) {
                    if (!file.isFile()) {
                        continue;
                    }
                    config.put("url", file.toURI().toString());
                    crawl(config, output, isOnlyDump, dumpRoot, fields);
                }
            }
            return;
        }

        if (configIndex >= 0) {
            crawl(configs[configIndex], output, isOnlyDump, dumpRoot, fields);
            return;
        }

        for (final JSONObject config : configs) {
            if (isParallel) {
                final String fixedOutput = output;
                final boolean fixedIsDumpOnly = isOnlyDump;
                final String fixedDumpRoot = dumpRoot;
                new Thread(() -> {
                    try {
                        crawl(config, fixedOutput, fixedIsDumpOnly, fixedDumpRoot, fields);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                crawl(config, output, isOnlyDump, dumpRoot, fields);
            }
        }
    }
}
