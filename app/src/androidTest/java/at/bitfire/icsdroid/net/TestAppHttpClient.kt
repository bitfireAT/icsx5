package at.bitfire.icsdroid.net

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.bitfire.icsdroid.AppHttpClient
import at.bitfire.icsdroid.MockServer
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull

@RunWith(AndroidJUnit4::class)
class TestAppHttpClient {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var client: AppHttpClient

    @Before
    fun setUp() {
        MockServer.clear()
        client = MockServer.httpClient(context)
    }

    @After
    fun tearDown() {
        MockServer.clear()
    }

    // Verifies that no Accept-Charset header is sent by default
    @Test
    fun request_doesNotContainAcceptCharsetHeader() = runBlocking {
        // enqueue a simple 200 response
        MockServer.enqueue(content = "ok", status = HttpStatusCode.OK)

        // perform a GET request to the mock server
        val uri = MockServer.uri("test")
        val response: HttpResponse = client.httpClient.get(uri.toString())

        assertEquals(HttpStatusCode.OK, response.status)

        // retrieve the headers recorded by the mock server
        val headers = MockServer.lastRequestHeaders

        // Ensure headers were recorded
        assertNotNull(headers)

        // Assert that Accept-Charset header is not present
        assertFalse(headers!!.contains(HttpHeaders.AcceptCharset))
    }
}
