package at.bitfire.icsdroid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.internal.http.HttpDate
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.*

open class CalendarFetcher(
        val context: Context,
        var url: URL
): Runnable {

    companion object {
        const val MIME_CALENDAR_OR_OTHER = "text/calendar, */*;q=0.9"
        const val MAX_REDIRECT_COUNT = 5
    }

    // TODO custom certificates
    // TODO authentication

    private var redirectCount = 0

    var ifModifiedSince: Long? = null
    var ifNoneMatch: String? = null


    override fun run() {
        if (url.protocol.equals("file", true))
            fetchFile()
        else
            fetchNetwork()
    }

    open fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?) {
    }

    open fun onRedirect(httpCode: Int, target: URL?) {
        url = target ?: throw IOException("Got redirect without target")

        // only network resources can be redirected
        if (++redirectCount < MAX_REDIRECT_COUNT)
            fetchNetwork()
        else
            onError(IOException("More than $MAX_REDIRECT_COUNT redirect"))
    }

    open fun onError(error: Exception) {
    }


    private fun fetchFile() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            throw IOException(context.getString(R.string.sync_permission_required))

        try {
            File(url.toURI()).let { file ->
                file.inputStream().use { content ->
                    onSuccess(content, null, null, file.lastModified())
                }
            }
        } catch (e: Exception) {
            onError(e)
        }
        throw NotImplementedError()
    }

    private fun fetchNetwork() {
        val request = Request.Builder()
                .addHeader("Accept", MIME_CALENDAR_OR_OTHER)
                .url(url)

        ifModifiedSince?.let {
            request.addHeader("If-Modified-Since", HttpDate.format(Date(it)))
        }
        ifNoneMatch?.let {
            request.addHeader("If-None-Match", it)
        }

        // TODO authentication

        try {
            val response = HttpClient.okHttpClient.newCall(request.build()).execute()
            if (response.isSuccessful)
                response.body()?.use { body ->
                    onSuccess(
                            body.byteStream(),
                            body.contentType(),
                            response.header("ETag"),
                            response.header("Last-Modified")?.let {
                                HttpDate.parse(it)?.time
                            }
                    )
                }
            else if (response.isRedirect || response.code() == 304)
                onRedirect(response.code(), response.header("Location")?.let { location ->
                    URL(url, location)
                })
        } catch (e: Exception) {
            onError(e)
        }
    }

}