/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import at.bitfire.icsdroid.HttpUtils.toURI
import at.bitfire.icsdroid.HttpUtils.toUri
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.Request
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.util.*

open class CalendarFetcher(
        val context: Context,
        var uri: Uri
): Runnable {

    companion object {
        const val MIME_CALENDAR_OR_OTHER = "text/calendar, */*;q=0.9"
        const val MAX_REDIRECT_COUNT = 5
    }

    private var redirectCount = 0
    private var hasFollowedTempRedirect = false

    var ifModifiedSince: Long? = null
    var ifNoneMatch: String? = null

    var username: String? = null
    var password: String? = null

    var inForeground = false


    override fun run() {
        if (uri.scheme.equals("http", true) or uri.scheme.equals("https", true))
            fetchNetwork()
        else
            fetchLocal()
    }

    open fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?, displayName: String?) {
    }

    open fun onNotModified() {
    }

    open fun onRedirect(httpCode: Int, target: Uri) {
        Log.v(Constants.TAG, "Get redirect $httpCode to $target")

        // only network resources can be redirected
        if (++redirectCount > MAX_REDIRECT_COUNT)
            throw IOException("More than $MAX_REDIRECT_COUNT redirect")

        // don't allow switching from HTTPS to a potentially insecure protocol (like HTTP)
        if (uri.scheme.equals("https", true) && !target.scheme.equals("https", true))
            throw IOException("Received redirect from HTTPS to ${target.scheme}")

        // update URL
        uri = target

        // call onNewPermanentUrl if this is a permanent redirect and we've never followed a temporary redirect
        if (!hasFollowedTempRedirect) {
            when (httpCode) {
                // 301: Moved Permanently, 308: Permanent Redirect
                HttpURLConnection.HTTP_NOT_MODIFIED,
                HttpUtils.HTTP_PERMANENT_REDIRECT ->
                    onNewPermanentUrl(target)
                else ->
                    hasFollowedTempRedirect = true
            }
        }

        fetchNetwork()
    }

    open fun onNewPermanentUrl(target: Uri) {
    }

    open fun onError(error: Exception) {
    }


    /**
     * Fetch the file with Android SAF
     */
    internal fun fetchLocal() {
        Log.i(Constants.TAG, "Fetching local file $uri")
        try {
            val contentResolver = context.contentResolver

            // We could check LAST_MODIFIED from the DocumentProvider here, but it's not clear whether it's reliable enough
            var displayName: String? = null
            contentResolver.query(
                uri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_LAST_MODIFIED),
                null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    displayName = cursor.getString(0)
                    //lastModified = cursor.getLong(1)
                    Log.i(Constants.TAG, "Get metadata from SAF: displayName = $displayName")
                }
            }

            contentResolver.openInputStream(uri)?.use { inputStream ->
                onSuccess(inputStream, null, null, null, displayName)
            }
        } catch (e: FileNotFoundException) {
            // file not there (anymore)
            onError(IOException(context.getString(R.string.could_not_open_storage_file)))
        } catch (e: SecurityException) {
            // no access to file (anymore)
            onError(IOException(context.getString(R.string.could_not_open_storage_file)))
        } catch (e: Exception) {
            // other error
            Log.e(Constants.TAG, "Couldn't open SAF document", e)
            onError(e)
        }
    }

    /**
     * Fetch the file over network
     */
    internal fun fetchNetwork() {
        Log.i(Constants.TAG, "Fetching remote file $uri")
        val request = Request.Builder()
                .addHeader("Accept", MIME_CALENDAR_OR_OTHER)
                .url(uri.toString())

        val currentUsername = username
        val currentPassword = password
        if (currentUsername != null && currentPassword != null)
            request.addHeader("Authorization", Credentials.basic(currentUsername, currentPassword, Charsets.UTF_8))

        ifModifiedSince?.let {
            request.addHeader("If-Modified-Since", HttpUtils.formatDate(Date(it)))
        }
        ifNoneMatch?.let {
            request.addHeader("If-None-Match", it)
        }

        try {
            HttpClient.get(context).okHttpClient.newCall(request.build()).execute().use { response ->
                when {
                    // 20x
                    response.isSuccessful -> response.body?.let { body ->
                        onSuccess(
                                body.byteStream(),
                                body.contentType(),
                                response.header("ETag"),
                                response.header("Last-Modified")?.let {
                                    HttpUtils.parseDate(it)?.time
                                },
                            null
                        )
                    }

                    // 30x
                    response.isRedirect -> {
                        val location = response.header("Location")
                        if (location != null) {
                            val newUri = uri.toURI().resolve(location)
                            onRedirect(response.code, newUri.toUri())
                        }
                        else
                            throw IOException("Got ${response.code} ${response.message} without Location")
                    }
                    response.code == HttpURLConnection.HTTP_NOT_MODIFIED -> onNotModified()

                    // HTTP error
                    else -> throw IOException("HTTP ${response.code} ${response.message}")
                }
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

}
