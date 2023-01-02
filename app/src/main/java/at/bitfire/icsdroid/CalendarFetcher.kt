/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.WorkerThread
import at.bitfire.icsdroid.HttpUtils.toURI
import at.bitfire.icsdroid.HttpUtils.toUri
import at.bitfire.icsdroid.exceptions.HttpInvalidResponseException
import at.bitfire.icsdroid.exceptions.HttpServerException
import at.bitfire.icsdroid.exceptions.IOSecurityException
import at.bitfire.icsdroid.exceptions.MaxRedirectCountException
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.executeAsync
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.util.*

/**
 * Provides an utility for fetching the events data from a server's or local uri.
 * @since 20221228
 * @param context The context that is making the request.
 * @param uri The target uri to make the request to. Supports servers (http/https) or local files.
 */
open class CalendarFetcher(
    val context: Context,
    var uri: Uri,
) {

    companion object {
        /**
         * The MIME type that accepts calendar or other types.
         * @since 20221228
         */
        const val MIME_CALENDAR_OR_OTHER = "text/calendar, */*;q=0.9"

        /**
         * The maximum redirection count allowed. When reached, [CalendarFetcher.onError] will throw [MaxRedirectCountException].
         * @since 20221228
         */
        const val MAX_REDIRECT_COUNT = 5
    }

    /**
     * Stores the amount of redirections made.
     * @since 20221228
     */
    private var redirectCount = 0

    /**
     * Stores if the redirection made is temporal or permanent.
     * @since 20221228
     */
    private var hasFollowedTempRedirect = false

    var ifModifiedSince: Long? = null
    var ifNoneMatch: String? = null

    var username: String? = null
    var password: String? = null

    var inForeground = false

    /**
     * Fetches the data from the [uri] given. Awaits until the fetch is completed. Errors are thrown through [onError].
     * @since 20221228
     */
    @WorkerThread
    suspend fun fetch() {
        try {
            if (uri.scheme.equals("http", true) or uri.scheme.equals("https", true))
                fetchNetwork()
            else
                fetchLocal()
        } catch (e: FileNotFoundException) {
            // file not there (anymore)
            onError(FileNotFoundException(context.getString(R.string.could_not_open_storage_file)))
        } catch (e: SecurityException) {
            // no access to file (anymore)
            onError(SecurityException(context.getString(R.string.could_not_open_storage_file), e))
        } catch (e: Exception) {
            // other error
            Log.e(Constants.TAG, "Couldn't open SAF document", e)
            onError(e)
        }
    }

    /**
     * Gets called when the data has been successfully fetched.
     * @since 20221228
     * @param data The data read from the target uri. You can use a [InputStreamReader] together
     *             with [Event.eventsFromReader] to process the events. You don't need to close this stream.
     * @param contentType The [MediaType] of the result. Only available when fetching from remote server.
     * @param eTag The eTag header of the response. Only available when fetching from remote server.
     * @param lastModified The last modification timestamp header. Only available when fetching from remote server.
     * @param displayName The display name of the file. Only available when fetching from local file.
     */
    @WorkerThread
    open suspend fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?, displayName: String?) {
    }

    /**
     * Gets called when fetching a server, and it returns a `HTTP_NOT_MODIFIED` result.
     * @since 20221228
     */
    @WorkerThread
    open suspend fun onNotModified() {
    }

    /**
     * Called when the server gives a redirection response.
     * @since 20221228
     * @param httpCode The response code that the server has given.
     * @param target The target uri the server is requesting a redirection to.
     * @throws MaxRedirectCountException When the redirection count has reached [MAX_REDIRECT_COUNT].
     * @throws IOSecurityException When there's a redirection from https to http.
     * @see onNewPermanentUrl
     */
    @WorkerThread
    @Throws(MaxRedirectCountException::class, IOSecurityException::class)
    open suspend fun onRedirect(httpCode: Int, target: Uri) {
        Log.v(Constants.TAG, "Get redirect $httpCode to $target")

        // only network resources can be redirected
        if (++redirectCount > MAX_REDIRECT_COUNT)
            throw MaxRedirectCountException("More than $MAX_REDIRECT_COUNT redirect")

        // don't allow switching from HTTPS to a potentially insecure protocol (like HTTP)
        if (uri.scheme.equals("https", true) && !target.scheme.equals("https", true))
            throw IOSecurityException("Received redirect from HTTPS to ${target.scheme}")

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

    /**
     * Called when received a permanent redirection.
     * @since 20221228
     * @param target The target uri the redirection is targeting to.
     */
    @WorkerThread
    open suspend fun onNewPermanentUrl(target: Uri) {
    }

    /**
     * Called whenever there's an error.
     *
     * Network exceptions:
     * - [HttpServerException]: When the server has given an error response.
     * - [HttpInvalidResponseException]: When a redirection response given by the server is not valid. This is a
     * redirection without the `Location` header.
     *
     * Local file exceptions:
     * - [FileNotFoundException]: When the file selected doesn't exist.
     * - [SecurityException]: When there's no longer access to the file selected.
     * - [IllegalStateException]: If the content resolver returned a `null` [InputStream] for the given [uri].
     * @since 20221228
     * @param error The exception thrown.
     */
    @WorkerThread
    open suspend fun onError(error: Exception) {
    }

    /**
     * Fetch the file with Android SAF. Calls [onSuccess] when finished.
     * @since 20221228
     * @throws FileNotFoundException When the file selected doesn't exist.
     * @throws SecurityException When there's no longer access to the file selected.
     * @throws IllegalStateException If the content resolver returned a `null` [InputStream] for the given [uri].
     */
    @WorkerThread
    @Throws(FileNotFoundException::class, SecurityException::class, IllegalStateException::class)
    private suspend fun fetchLocal() {
        Log.i(Constants.TAG, "Fetching local file $uri")

        val contentResolver = context.contentResolver

        // We could check LAST_MODIFIED from the DocumentProvider here, but it's not clear whether it's reliable enough
        var displayName: String? = null
        contentResolver.query(
            uri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_LAST_MODIFIED),
            null, null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                displayName = cursor.getString(0)
                //lastModified = cursor.getLong(1)
                Log.i(Constants.TAG, "Get metadata from SAF: displayName = $displayName")
            }
        }

        contentResolver.openInputStream(uri)?.use { inputStream ->
            onSuccess(inputStream, null, null, null, displayName)
        } ?: throw IllegalStateException("Could not open input stream for the given URI.")
    }

    /**
     * Fetch the file over network.
     * @since 20221228
     * @throws HttpServerException When the server has given an error response.
     * @throws HttpInvalidResponseException When a redirection response given by the server is not valid. This is a redirection without the `Location` header.
     */
    @WorkerThread
    @Throws(HttpServerException::class, HttpInvalidResponseException::class)
    private suspend fun fetchNetwork() {
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

        HttpClient.get(context)
            .okHttpClient
            .newCall(request.build())
            .executeAsync()
            .use { response ->
                when {
                    // 20x
                    response.isSuccessful ->
                        onSuccess(
                            response.body.byteStream(),
                            response.body.contentType(),
                            response.header("ETag"),
                            response.header("Last-Modified")?.let {
                                HttpUtils.parseDate(it)?.time
                            },
                            null
                        )

                    // 30x
                    response.isRedirect -> {
                        val location = response.header("Location")
                        if (location != null) {
                            val newUri = uri.toURI().resolve(location)
                            onRedirect(response.code, newUri.toUri())
                        } else
                            throw HttpInvalidResponseException("Got ${response.code} ${response.message} without Location")
                    }
                    response.code == HttpURLConnection.HTTP_NOT_MODIFIED -> onNotModified()

                    // HTTP error
                    else -> throw HttpServerException("HTTP ${response.code} ${response.message}")
                }
            }
    }

}
