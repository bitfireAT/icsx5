package at.bitfire.icsdroid

import okhttp3.MediaType
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL

open class CalendarFetcher(
        var url: URL
): Runnable {

    companion object {
        const val MIME_CALENDAR_OR_OTHER = "text/calendar, */*;q=0.9"
        const val MAX_REDIRECT_COUNT = 5
    }

    // TODO custom certificates
    // TODO authentication
    // TODO file support

    private var redirectCount = 0


    override fun run() {
        if (url.protocol.equals("file", true))
            fetchFile()
        else
            fetchNetwork()
    }

    open fun onSuccess(data: InputStream, contentType: MediaType?) {
    }

    open fun onRedirect(target: URL) {
        url = target

        // only network resources can be redirected
        if (++redirectCount < MAX_REDIRECT_COUNT)
            fetchNetwork()
        else
            onError(IOException("More than $MAX_REDIRECT_COUNT redirect"))
    }

    open fun onError(error: Exception) {
    }


    private fun fetchFile() {
        try {
            File(url.toURI()).inputStream().use { content ->
                onSuccess(content, null)
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
        try {
            val response = HttpClient.okHttpClient.newCall(request.build()).execute()
            if (response.isSuccessful)
                response.body()?.use { body ->
                    onSuccess(body.byteStream(), body.contentType())
                }
            else if (response.isRedirect)
                response.header("Location")?.let { location ->
                    onRedirect(URL(url, location))
                }
            else
                throw IOException("${response.code()} ${response.message()}")
        } catch (e: Exception) {
            onError(e)
        }
    }

}