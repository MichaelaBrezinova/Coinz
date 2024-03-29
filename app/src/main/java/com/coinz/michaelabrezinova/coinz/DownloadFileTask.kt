package com.coinz.michaelabrezinova.coinz

import android.os.AsyncTask
import java.net.URL
import com.mapbox.geojson.FeatureCollection
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import org.json.JSONException
import org.json.JSONArray

class DownloadFileTask(private val caller: DownloadCompleteListener):
        AsyncTask<String,Void,String>() {

    override fun doInBackground(vararg urls: String): String = try{
        loadFileFromNetwork(urls[0])
    } catch (e: IOException) {
        "Unable to load content. Check your network connection"
    }

    //Load the file specified by the url in the background, return the result string
    private fun loadFileFromNetwork(urlString: String): String {
        val stream: InputStream = downloadUrl(urlString)
        //Read input from stream
        val reader = BufferedReader(InputStreamReader(stream))
        val result = StringBuilder()
        var line: String? = reader.readLine()
        while(line!=null) {
            result.append(line)
            line = reader.readLine()
        }
        return result.toString()
    }

    //Given a string representation of a URL, sets up a connection and gets an input stream
    @Throws(IOException::class)
    private fun downloadUrl(urlString: String): InputStream {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.readTimeout = 10000
        conn.connectTimeout = 15000
        conn.requestMethod = "GET"
        conn.doInput = true
        conn.connect() //Starts the query
        return conn.inputStream
    }

    //Call download complete
    override fun onPostExecute(result: String) {
        super.onPostExecute(result)

        caller.downloadComplete(result)
    }
}

interface DownloadCompleteListener {
    fun downloadComplete(result: String)
}

//On download complete, set the variables from MapsActivity - list of features and geoJson string
object DownloadCompleteRunner: DownloadCompleteListener {
    private var result: String = ""
    override fun downloadComplete(result: String) {
        this.result = result
        if (isStringJson(result)){
            MapsActivity.downloadedGJson = result
            val fc = FeatureCollection.fromJson(result)
            if(fc!=null) {
                MapsActivity.features = fc.features()!!.toCollection(ArrayList())
                val obj = JSONObject(result)
                MapsActivity.rates = obj.getJSONObject("rates")
            }
        }
    }
}

//Check if the result string is proper json string to prevent errors
fun isStringJson(result: String): Boolean {
    try {
        JSONObject(result)
    } catch (ex: JSONException) {
        try {
            JSONArray(result)
        } catch (ex1: JSONException) {
            return false
        }
    }
    return true
}