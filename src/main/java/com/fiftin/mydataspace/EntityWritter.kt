package com.fiftin.mydataspace

import com.fiftin.CSVWriter
import com.fiftin.MyWriter
import com.fiftin.Pair
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject

import java.io.IOException
import java.io.PrintWriter
import okhttp3.OkHttpClient
import java.security.cert.CertificateException
import javax.net.ssl.*


/**
 * Saves data to MyDataSpace.
 * @author Denis Gukov &lt;denguk@gmail.com&gt;
 */
class EntityWriter(private val fields: Map<String, String>) : MyWriter {
    private val client: OkHttpClient = getUnsafeOkHttpClient()
    private val logWriter = CSVWriter(PrintWriter(System.out))
    private val apiBase = "https://api.mydataspace.net"
    private var authorizationString: String? = null

    private val fieldRegex = Regex("^([^$]+)\\$(\\w)$")
    private val fieldNameRegex = Regex("^([\\w\\d_-]+)-\\>([\\w\\d_-]+):(.*)$")

    private var children: JSONArray = JSONArray()

    override fun writeHeader(columnNames: Collection<String>) {
        logWriter.writeHeader(columnNames)
    }

    private fun getUnsafeOkHttpClient(): OkHttpClient {
        // Create a trust manager that does not validate certificate chains
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
            }

            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate>? {
                return arrayOf()
            }
        })

        // Install the all-trusting trust manager
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        // Create an ssl socket factory with our all-trusting manager
        val sslSocketFactory = sslContext.socketFactory

        val builder = OkHttpClient().newBuilder().sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager?)
        builder.hostnameVerifier { _, _ -> true }
        return builder.build()
    }

    @Throws(IOException::class)
    override fun auth() {
        val url = apiBase + "/auth?authProvider=access-token&" +
                "state=permission%3dadmin%26clientId%3d" + fields["client_id"] + "%26resultFormat=json&" +
                "accessToken=" + fields["access_token"]
        val request = Request.Builder()
                .url(url)
                .get()
                .build()
        client.newCall(request).execute().use { response -> authorizationString = JSONObject(response.body().string()).getString("jwt") }

        var dataURL = apiBase + "/v1/entities?children=true&limit=100&root=" + fields["root"] + "&path=" + fields["path"];
        val dataRequest = Request.Builder()
                .url(dataURL)
                .get()
                .build()
        client.newCall(dataRequest).execute().use { response -> children = JSONObject(response.body().string()).getJSONArray("children") }
    }

    private fun findFieldOfEntity(entity: JSONObject, name: String): JSONObject? {
        val fields = entity.getJSONArray("fields")
        return (0 until fields.length())
                .map { fields.getJSONObject(it) }
                .firstOrNull { it.getString("name") == name }
    }

    private fun findEntityByField(name: String, value: String): JSONObject? {
        for (i in 0 until children.length()) {
            val entity = children.getJSONObject(i)
            val field = findFieldOfEntity(entity, name)
            if (field != null && field.getString("value") == value) {
                return entity
            }
        }
        return null
    }

    override fun write(content: List<Pair<String, List<String>>>): Int {
        val url = apiBase + "/v1/entities"

        val nameField = content.find { f -> f.key == "\$name" }
        if (nameField == null) {
            println("\$name field not fund")
            return 0
        }

        var written: Int = 0

        for (i in 0 until nameField.value.size) {
            val jEntity = JSONObject()
            val jFields = JSONArray()
            jEntity.put("root", fields["root"])
            jEntity.put("fields", jFields)
            try {
                for (field in content) {
                    var value: String? = null
                    if (i < field.value.size) {
                        value = field.value[i]
                    }

                    if (field.key == "\$name") {
                        if (value == null) {
                            continue
                        }
                        val m = fieldNameRegex.matchEntire(value)
                        if (m != null) {
                            val entity = findEntityByField(m.groupValues[1], m.groupValues[3]) ?: throw Error("Entity not found")
                            val name = findFieldOfEntity(entity, m.groupValues[2])?.getString("value") ?: Error("Field ${m.groupValues[2]} is not exists")
                            jEntity.put("path", fields["path"] + "/" + name)
                        } else {
                            jEntity.put("path", fields["path"] + "/" + value)
                        }
                        continue
                    }
                    if (field.key.startsWith("$")) {
                        continue
                    }
                    val jField = JSONObject()
                    val m = fieldRegex.matchEntire(field.key)
                    if (m != null) {
                        jField.put("name", m.groupValues[1])
                        jField.put("type", m.groupValues[2])
                    } else {
                        jField.put("name", field.key)
                        jField.put("type", "s")
                    }
                    jField.put("value", value)
                    jFields.put(jField)
                }

                if (!jEntity.has("path")) {
                    println("Data does not saved. Content has no field '\$name'")
                    continue
                }

                val sEntity = jEntity.toString()

                val request = Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer $authorizationString")
                        .post(RequestBody.create(JSON, sEntity))
                        .build()

                try {
                    val response = client.newCall(request).execute()
                    val name = jEntity.getString("path")

                    println(name + " => " + response.code())
                    println(response.body().string())

                    if (response.code() != 200) {
                        println(response.body().string())
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                written++
            } catch (e: Error) {
                println(e.message)
            }
        }

        logWriter.write(content)
        return written
    }

    companion object {
        private val JSON = MediaType.parse("application/json; charset=utf-8")
    }
}
