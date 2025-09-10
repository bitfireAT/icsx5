/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import at.bitfire.icsdroid.HttpUtils.toURI
import at.bitfire.icsdroid.HttpUtils.toUri
import at.bitfire.icsdroid.UriUtils.toURL
import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.ClosedByteChannelException
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.Date

open class CalendarFetcher(
    val context: Context,
    var uri: Uri,
    val client: AppHttpClient
) {

    private var redirectCount = 0
    private var hasFollowedTempRedirect = false

    var ifModifiedSince: Long? = null
    var ifNoneMatch: String? = null

    var username: String? = null
    var password: String? = null

    var inForeground = false


    suspend fun fetch() {
        if (uri.scheme.equals("http", true) or uri.scheme.equals("https", true))
            fetchNetwork()
        else
            fetchLocal()
    }

    open suspend fun onSuccess(data: InputStream, contentType: ContentType?, eTag: String?, lastModified: Long?, displayName: String?) {
    }

    open suspend fun onNotModified() {
    }

    open suspend fun onRedirect(httpCode: HttpStatusCode, target: Uri) {
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
                HttpStatusCode.NotModified,
                HttpStatusCode.PermanentRedirect ->
                    onNewPermanentUrl(target)
                else ->
                    hasFollowedTempRedirect = true
            }
        }

        fetchNetwork()
    }

    open suspend fun onNewPermanentUrl(target: Uri) {
    }

    open suspend fun onError(error: Exception) {
    }


    /**
     * Fetch the file with Android SAF
     */
    internal suspend fun fetchLocal() {
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
            onError(IOException(context.getString(R.string.could_not_open_storage_file), e))
        } catch (e: SecurityException) {
            // no access to file (anymore)
            onError(IOException(context.getString(R.string.could_not_open_storage_file), e))
        } catch (e: Exception) {
            // other error
            Log.e(Constants.TAG, "Couldn't open SAF document", e)
            onError(e)
        }
    }

    /**
     * Fetch the file over network
     */
    internal suspend fun fetchNetwork() {
        Log.i(Constants.TAG, "Fetching remote file $uri")
        try {
            client.httpClient.get(uri.toURL()) {
                header(HttpHeaders.Accept, MIME_CALENDAR_OR_OTHER)

                val currentUsername = username
                val currentPassword = password
                if (currentUsername != null && currentPassword != null)
                    basicAuth(currentUsername, currentPassword)

                ifModifiedSince?.let {
                    header(HttpHeaders.IfModifiedSince, HttpUtils.formatDate(Date(it)))
                }
                ifNoneMatch?.let {
                    header(HttpHeaders.IfNoneMatch, it)
                }
            }.let { response ->
                val statusCode = response.status
                when {
                    // 20x
                    statusCode.isSuccess() ->
                        onSuccess(
                            response.bodyAsChannel().toInputStream(),
                            response.contentType(),
                            response.headers[HttpHeaders.ETag],
                            response.headers[HttpHeaders.LastModified]?.let {
                                HttpUtils.parseDate(it)?.time
                            },
                            null
                        )

                    // 30x
                    statusCode == HttpStatusCode.NotModified -> onNotModified()
                    statusCode.value in 300..399 -> {
                        val location = response.headers[HttpHeaders.Location]
                        if (location != null) {
                            val newUri = uri.toURI().resolve(location)
                            onRedirect(statusCode, newUri.toUri())
                        }
                        else
                            throw IOException("Got ${statusCode.value} ${statusCode.description} without Location")
                    }

                    // HTTP error
                    else -> throw IOException("HTTP ${statusCode.value} ${statusCode.description}")
                }
            }
        } catch (e: ClosedByteChannelException) {
            // Ignore ClosedByteChannelException which is thrown ProtocolException is thrown which
            // happens when when servers misbehave and for example send more bytes than expected.
            Log.i(Constants.TAG, "Ignoring ClosedByteChannelException", e)
        } catch (e: Exception) {
            onError(e)
        }
    }

    companion object {
        const val MIME_CALENDAR_OR_OTHER = "text/calendar, */*;q=0.9"
        const val MAX_REDIRECT_COUNT = 5
    }

}
