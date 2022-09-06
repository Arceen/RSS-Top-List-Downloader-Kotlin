package com.example.topdownloader

import android.content.Context
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.recyclerview.widget.RecyclerView
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import kotlin.properties.Delegates

class FeedEntry {
    var name: String = ""
    var artist: String = ""
    var releaseDate: String = ""
    var summary: String = ""
    var imageURL: String = ""
    override fun toString(): String {
        return """
            name = $name
            artist = $artist
            releaseDate = $releaseDate
            imageURL = $imageURL
        """.trimIndent()
    }
}

class MainActivity : AppCompatActivity() {
    private val TAG = "TagMainActivity"
    private var downloadData: DownloadData? = null
    private var feedUrl: String = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml"
    private var feedLimit = 10
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState != null) {
            feedLimit = savedInstanceState?.getInt("feedLimit")!!
            feedUrl = savedInstanceState?.getString("feedUrl")!!
            Log.e("faketag", "Restoring shit $feedUrl")
        }
        downloadData = DownloadData( this, findViewById(R.id.xmlListView))

        downloadData?.execute(feedUrl.format(feedLimit))



    }


    override fun onSaveInstanceState(outState: Bundle) {

        super.onSaveInstanceState(outState)
        Log.e("adsf1","storing shit")
        outState.putString("feedUrl", feedUrl)
        outState.putInt("feedLimit", feedLimit)

    }

    private fun downloadUrl(feedUrl: String){
        downloadData = DownloadData( this, findViewById(R.id.xmlListView))

        downloadData?.execute(feedUrl)

    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.feeds_menu, menu)
        if (feedLimit == 10) {
            menu?.findItem(R.id.mnu10)?.isChecked = true
        } else {
            menu?.findItem(R.id.mnu25)?.isChecked = true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var newFeedUrl: String = feedUrl
        var newFeedLimit = feedLimit
        when (item.itemId){
            R.id.mnuFree -> newFeedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
            R.id.mnuPaid -> newFeedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml"
            R.id.mnuSongs -> newFeedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml"
            R.id.mnu10, R.id.mnu25 -> {
                if (!item.isChecked) {
                    item.isChecked = true
                    newFeedLimit = 35-newFeedLimit
                }
            }
            else -> return super.onOptionsItemSelected(item)
        }
        if (feedUrl == newFeedUrl && newFeedLimit == feedLimit){
            //don't download again
            return true
        } else {
            feedLimit = newFeedLimit
            feedUrl = newFeedUrl
            downloadUrl(feedUrl.format(feedLimit))
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadData?.cancel(true)
    }

    companion object {
        private class DownloadData(context: Context, listView: ListView): AsyncTask<String, Void, String>() {
            private val TAG = "TagDownloadData"
            var propContext: Context by Delegates.notNull()
            var propListView: ListView by Delegates.notNull()

            init {
                propContext = context
                propListView = listView
            }

            override fun doInBackground(vararg url: String?): String {
                Log.d(TAG, "doInBackground: starts with ${url[0]}")
                val rssFeed = downloadXML(url[0])
                if (rssFeed.isEmpty()) {
                    Log.e(TAG, "doInBackground: Error downloading")
                }
                return rssFeed
            }

            override fun onPostExecute(result: String?) {
                super.onPostExecute(result)
//                Log.d(TAG, "onPostExecute: parameter is done")
                val parseApplications = ParseApplications()
                parseApplications.parse(result!!)
//                val arrayAdaptor = ArrayAdapter<FeedEntry>(propContext, R.layout.list_item, parseApplications.applications )
//                propListView.adapter = arrayAdaptor
                val feedAdapter = FeedAdapter(propContext, R.layout.list_record, parseApplications.applications)
                propListView.adapter = feedAdapter
            }

            private fun downloadXML(urlPath: String?): String {
                Log.e("asdf", urlPath!!)
                return URL(urlPath).readText()
            }

        }
    }

    private fun downloadXML(urlPath: String?): String {
        val xmlResult = StringBuilder()
        try {
            val url = URL(urlPath)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            val response = connection.responseCode
            Log.d(TAG, "downloadXML: The response code was $response")

//            val reader = BufferedReader(InputStreamReader(connection.inputStream))
//            val inputBuffer = CharArray(500)
//            var charsRead = 0
//            while (charsRead >= 0) {
//                charsRead = reader.read(inputBuffer)
//                if (charsRead > 0) {
//                    xmlResult.append(String(inputBuffer, 0, charsRead))
//                }
//            }
//            reader.close()

            connection.inputStream.buffered().reader().use { xmlResult.append(it.readText()) }
            Log.d(TAG, "Received ${xmlResult.length} bytes")
            return xmlResult.toString()
//        } catch (e: MalformedURLException) {
//            Log.e(TAG, "downloadXML: Invalid URL ${e.message}")
//        } catch (e: IOException) {
//            Log.e(TAG, "downloadXML: IO Exception reading data: ${e.message}")
//        } catch(e: SecurityException){
//            Log.e(TAG, "downloadXML: Security Exception")
//        }
//        catch (e: Exception) {
//            Log.e(TAG, "downloadXML: Exception: ${e.message}")
//        }
        } catch (e: Exception){
            val errorMessage = when(e){
                is MalformedURLException -> "Invalid URL "
                is IOException -> "IO Exception reading data "
                is SecurityException -> "Security Exception "
                else -> "Other Exception: "
            }
            Log.d(TAG, errorMessage+"${e.message}")
        }
        return ""
    }
}
