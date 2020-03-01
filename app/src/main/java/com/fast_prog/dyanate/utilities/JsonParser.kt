package com.fast_prog.dyanate.utilities

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.*

class JsonParser {

    internal var charset = "UTF-8"
    internal lateinit var conn: HttpURLConnection
    internal lateinit var wr: DataOutputStream
    internal var result = StringBuilder()
    internal lateinit var urlObj: URL
    internal var jObj: JSONObject? = null
    internal lateinit var sbParams: StringBuilder
    internal lateinit var paramsString: String

    fun makeHttpRequest(url: String, method: String,
                        params: HashMap<String, String>): JSONObject? {
        var url = url

        Log.d("url_", url)

        sbParams = StringBuilder()
        var i = 0
        for (key in params.keys) {
            try {
                if (i != 0) {
                    sbParams.append("&")
                }
                sbParams.append(key).append("=")
                        .append(URLEncoder.encode(params[key], charset))

            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

            i++
        }
        Log.d("url_", sbParams.toString())
        if (method == "POST") {
            //Log.d("request method is POST", "PSPRT");
            try {
                urlObj = URL(url)

                conn = urlObj.openConnection() as HttpURLConnection

                conn.doOutput = true

                conn.requestMethod = "POST"

                conn.setRequestProperty("Connection", "Keep-Alive")
                conn.setRequestProperty("Accept-Charset", charset)

                conn.readTimeout = 30000
                conn.connectTimeout = 35000

                conn.connect()

                paramsString = sbParams.toString()

                wr = DataOutputStream(conn.outputStream)
                wr.writeBytes(paramsString)
                wr.flush()
                wr.close()

            } catch (e: IOException) {
                e.printStackTrace()
            }

        } else if (method == "GET") {
            //Log.d("request method is GETY", "GET");

            if (sbParams.length != 0) {
                url += "?" + sbParams.toString()
            }

            try {
                urlObj = URL(url)

                conn = urlObj.openConnection() as HttpURLConnection

                conn.doOutput = false

                conn.requestMethod = "GET"

                conn.setRequestProperty("Connection", "Keep-Alive")
                conn.setRequestProperty("Accept-Charset", charset)

                conn.connectTimeout = 15000

                conn.connect()

            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        try {
            //Receive the response from the server
            val `in` = BufferedInputStream(conn.inputStream)
            val reader = BufferedReader(InputStreamReader(`in`))

            var line: String? = null
            while ({ line = reader.readLine(); line }() != null) {
                result.append(line)
            }

            Log.d("url_", "result: " + result.toString())

        } catch (e: IOException) {
            e.printStackTrace()
        }

        conn.disconnect()

        // try parse the string to a JSON object
        try {
            jObj = JSONObject(result.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
            //Log.e("JSON Parser", "Error parsing data " + e.toString());
        }

        // return JSON Object
        return jObj
    }
}