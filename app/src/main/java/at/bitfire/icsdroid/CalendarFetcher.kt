/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.content.ContextCompat
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.Request
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
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
        if (uri.scheme.toString().equals("content", true))
            fetchContentUri()
        else
            fetchNetwork()
    }

    open fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?) {
    }

    open fun onNotModified() {
    }

    open fun onRedirect(httpCode: Int, target: URL) {
        Log.v(Constants.TAG, "Get redirect $httpCode to $target")

        // only network resources can be redirected
        if (++redirectCount > MAX_REDIRECT_COUNT)
            throw IOException("More than $MAX_REDIRECT_COUNT redirect")

        // don't allow switching from HTTPS to a potentially insecure protocol (like HTTP)
        if (URL(uri.toString()).protocol.equals("https", true) && !target.protocol.equals("https", true))
            throw IOException("Received redirect from HTTPS to ${target.protocol}")

        // update URL
        uri = Uri.parse(target.toString())

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

    open fun onNewPermanentUrl(target: URL) {
    }

    open fun onError(error: Exception) {
    }

    /**
     * Fetch the file with android SAF
     */
    private fun fetchContentUri() {
        try {

            val contentResolver = context.contentResolver
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)

            val lastModified: Long? = null
            contentResolver.query(
                uri, null, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val displayName = cursor.getString(columnIndex)
                    Log.i("TUG", "Display Name: $displayName")
                }
            }

            contentResolver.openInputStream(uri)?.use { inputStream ->
                onSuccess(inputStream, null, null, lastModified)
            }

        } catch (e: Exception) {
            onError(e)
        }
    }

    private fun fetchNetwork() {
        val request = Request.Builder()
                .addHeader("Accept", MIME_CALENDAR_OR_OTHER)
                .url(URL(uri.toString()))

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
                                }
                        )
                    }

                    // 30x
                    response.isRedirect -> {
                        val location = response.header("Location")
                        if (location != null)
                            onRedirect(response.code, URL(URL(uri.toString()), location))
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
