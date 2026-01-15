/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import at.bitfire.icsdroid.test.BuildConfig
import at.bitfire.icsdroid.test.R
import io.ktor.client.utils.buildHeaders
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.io.InputStream
import java.util.LinkedList

class CalendarFetcherTest {

    companion object {

        val appContext: Context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }
        val testContext: Context by lazy { InstrumentationRegistry.getInstrumentation().context }

    }

    private lateinit var client: AppHttpClient

    @Before
    fun setUp() {
        MockServer.clear()

        client = MockServer.httpClient(appContext)
    }

    @Test
    fun testFetchLocal_readsCorrectly() {
        val uri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${BuildConfig.APPLICATION_ID}/${R.raw.vienna_evolution}")

        var ical: String? = null
        val fetcher = object: CalendarFetcher(appContext, uri, client) {
            override suspend fun onSuccess(data: InputStream, contentType: ContentType?, eTag: String?, lastModified: Long?, displayName: String?) {
                ical = data.bufferedReader().use { it.readText() }
            }
        }
        runBlocking {
            fetcher.fetch()
        }

        testContext.resources.openRawResource(R.raw.vienna_evolution).use { streamCorrect ->
            val referenceData = streamCorrect.bufferedReader().use { it.readText() }
            assertEquals(referenceData, ical)
        }
    }

    @Test
    fun testFetchNetwork_success() {
        val etagCorrect = "33a64df551425fcc55e4d42a148795d9f25f89d4"
        val lastModifiedCorrect = "Wed, 21 Oct 2015 07:28:00 GMT"       // UNIX timestamp 1445405280
        val icalCorrect = testContext.resources.openRawResource(R.raw.vienna_evolution).use { streamCorrect ->
            streamCorrect.bufferedReader().use { it.readText() }
        }

        // create mock response
        MockServer.enqueue(
            content = icalCorrect,
            status = HttpStatusCode.OK,
            headers = buildHeaders {
                append(HttpHeaders.ETag, etagCorrect)
                append(HttpHeaders.LastModified, lastModifiedCorrect)
            }
        )

        // make request to local mock server
        var ical: String? = null
        var etag: String? = null
        var lastmod: Long? = null
        val fetcher = object: CalendarFetcher(appContext, MockServer.uri(), client) {
            override suspend fun onSuccess(data: InputStream, contentType: ContentType?, eTag: String?, lastModified: Long?, displayName: String?) {
                ical = data.bufferedReader().use { it.readText() }
                etag = eTag
                lastmod = lastModified
            }
        }
        runBlocking {
            fetcher.fetch()
        }

        // assert content, ETag and Last-Modified are correct
        assertEquals(etagCorrect, etag)
        assertEquals(1445412480000L, lastmod)
        assertEquals(icalCorrect, ical)
    }

    @Test
    fun testFetchNetwork_onRedirectWithLocation() {
        // create mock responses:
        // 1. redirect with absolute target URL
        MockServer.enqueue(
            status = HttpStatusCode.TemporaryRedirect,
            headers = headers {
                append(
                    HttpHeaders.Location, MockServer.uri("new-location", "vienna-evolution.ics").toString()
                )
            }
        )
        // 2. redirect with relative target URL
        MockServer.enqueue(
            status = HttpStatusCode.TemporaryRedirect,
            headers = headers {
                append(HttpHeaders.Location, "the-file-is-here")
            }
        )
        // 3. finally the real resource
        MockServer.enqueue(
            content = "icalCorrect",
            status = HttpStatusCode.OK
        )

        // make initial request to local mock server
        val baseUrl = MockServer.uri()
        var ical: String? = null
        val redirects = LinkedList<Uri>()
        val fetcher = object: CalendarFetcher(appContext, baseUrl, client) {
            override suspend fun onRedirect(httpCode: HttpStatusCode, target: Uri) {
                redirects += target
                super.onRedirect(httpCode, target)
            }
            override suspend fun onSuccess(data: InputStream, contentType: ContentType?, eTag: String?, lastModified: Long?, displayName: String?) {
                ical = data.bufferedReader().use { it.readText() }
            }
        }
        runBlocking {
            fetcher.fetch()
        }

        // assert redirects are made correctly
        assertArrayEquals(arrayOf(
            baseUrl.buildUpon()
                .appendPath("new-location")
                .appendPath("vienna-evolution.ics")
                .build(),
            baseUrl.buildUpon()
                .appendPath("new-location")
                .appendPath("the-file-is-here")
                .build()
        ), redirects.toTypedArray())

        // assert onSuccess is called
        assertEquals("icalCorrect", ical)
    }

    @Test
    fun testFetchNetwork_onRedirectWithoutLocation() {
        MockServer.enqueue(status = HttpStatusCode.TemporaryRedirect)

        var e: Exception? = null
        runBlocking {
            object : CalendarFetcher(appContext, MockServer.uri(), client) {
                override suspend fun onError(error: Exception) {
                    e = error
                }
            }.fetch()
        }

        assertEquals(IOException::class.java, e?.javaClass)
    }

    @Test
    fun testFetchNetwork_onNotModified() {
        MockServer.enqueue(status = HttpStatusCode.NotModified)

        var notModified = false
        runBlocking {
            object : CalendarFetcher(appContext, MockServer.uri(), client) {
                override suspend fun onNotModified() {
                    notModified = true
                }
            }.fetch()
        }

        assert(notModified)
    }

    @Test
    fun testFetchNetwork_onError() {
        MockServer.enqueue(status = HttpStatusCode.NotFound)

        var e: Exception? = null
        runBlocking {
            object : CalendarFetcher(appContext, MockServer.uri(), client) {
                override suspend fun onError(error: Exception) {
                    e = error
                }
            }.fetch()
        }

        assertEquals(IOException::class.java, e?.javaClass)
    }

    @Test
    fun testFetchNetwork_validContentType() {
        // Test that when a server returns a valid Content-Type header,
        // the fetcher correctly parses it and processes the calendar data with the proper contentType
        val icalCorrect = testContext.resources.openRawResource(R.raw.vienna_evolution).use { streamCorrect ->
            streamCorrect.bufferedReader().use { it.readText() }
        }

        // create mock response with valid Content-Type header
        MockServer.enqueue(
            content = icalCorrect,
            status = HttpStatusCode.OK,
            headers = buildHeaders {
                append(HttpHeaders.ContentType, "text/calendar; charset=utf-8")
            }
        )

        // make request to local mock server
        var ical: String? = null
        var receivedContentType: ContentType? = null
        val fetcher = object: CalendarFetcher(appContext, MockServer.uri(), client) {
            override suspend fun onSuccess(data: InputStream, contentType: ContentType?, eTag: String?, lastModified: Long?, displayName: String?) {
                ical = data.bufferedReader().use { it.readText() }
                receivedContentType = contentType
            }
        }
        runBlocking {
            fetcher.fetch()
        }

        // assert content is correct and contentType is properly parsed
        assertEquals(icalCorrect, ical)
        assertEquals("text/calendar; charset=utf-8", receivedContentType.toString())
    }

    @Test
    fun testFetchNetwork_malformedContentType() {
        // Test that when a server returns a malformed Content-Type header,
        // the fetcher logs a warning and successfully processes the calendar data with null contentType
        val icalCorrect = testContext.resources.openRawResource(R.raw.vienna_evolution).use { streamCorrect ->
            streamCorrect.bufferedReader().use { it.readText() }
        }

        // create mock response with malformed Content-Type header (containts "-" at start)
        // that will trigger BadContentTypeFormatException to be thrown
        MockServer.enqueue(
            content = icalCorrect,
            status = HttpStatusCode.OK,
            headers = buildHeaders {
                append(HttpHeaders.ContentType, "-Text/calendar charset=utf-8")
            }
        )

        // make request to local mock server
        var ical: String? = null
        var receivedContentType: ContentType? = null
        val fetcher = object: CalendarFetcher(appContext, MockServer.uri(), client) {
            override suspend fun onSuccess(data: InputStream, contentType: ContentType?, eTag: String?, lastModified: Long?, displayName: String?) {
                ical = data.bufferedReader().use { it.readText() }
                receivedContentType = contentType
            }
        }
        runBlocking {
            fetcher.fetch()
        }

        // assert content is correct and contentType is null (due to malformed header)
        assertEquals(icalCorrect, ical)
        assertEquals(null, receivedContentType)
    }

}