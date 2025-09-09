package at.bitfire.icsdroid

import android.content.Context
import at.bitfire.cert4android.CustomCertManager
import at.bitfire.icsdroid.HttpUtils.toUri
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.headersOf
import io.ktor.http.toURI
import java.util.concurrent.locks.ReentrantLock
import javax.net.ssl.SSLContext
import kotlin.concurrent.withLock

object MockServer {
    private val lock = ReentrantLock()

    private val queue = mutableListOf<Response>()

    val mockCreate: (CustomCertManager, SSLContext) -> HttpClientEngine = { _, _ ->
        MockEngine {
            if (queue.isNotEmpty()) {
                val response = lock.withLock { queue.removeAt(0) }
                respond(response.content, response.status, response.headers)
            } else {
                respond("No more responses", HttpStatusCode.NotImplemented)
            }
        }
    }

    fun clear() {
        queue.clear()
    }

    private fun enqueue(response: Response) {
        lock.withLock {
            queue.add(response)
        }
    }

    fun enqueue(
        content: String = "",
        status: HttpStatusCode = HttpStatusCode.OK,
        headers: Headers = headersOf()
    ) = enqueue(Response(content, status, headers))

    fun uri(vararg segments: String) = URLBuilder("http://localhost")
        .appendPathSegments(*segments)
        .build()
        .toURI()
        .toUri()

    fun httpClient(context: Context) = AppHttpClient(null, mockCreate, context)

    private class Response(val content: String, val status: HttpStatusCode, val headers: Headers)
}
