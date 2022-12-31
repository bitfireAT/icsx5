/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import at.bitfire.icsdroid.HttpUtils.toAndroidUri
import at.bitfire.icsdroid.exceptions.HttpInvalidResponseException
import at.bitfire.icsdroid.test.R
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.apache.commons.io.IOUtils
import org.junit.AfterClass
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.util.*

class CalendarFetcherTest {

    companion object {

        val appContext: Context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }
        val testContext: Context by lazy { InstrumentationRegistry.getInstrumentation().context }

        val server = MockWebServer()

        @BeforeClass
        @JvmStatic
        fun setUp() {
            server.start()
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            server.shutdown()
        }

    }

    @Test
    fun testFetchLocal_readsCorrectly() {
        val uri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${BuildConfig.APPLICATION_ID}/${R.raw.vienna_evolution}")

        var ical: String? = null
        val fetcher = object: CalendarFetcher(appContext, uri) {
            override suspend fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?, displayName: String?) {
                data.use { ical = IOUtils.toString(it, Charsets.UTF_8) }
            }
        }
        runBlocking {
            fetcher.fetch()
        }

        testContext.resources.openRawResource(R.raw.vienna_evolution).use { streamCorrect ->
            val referenceData = IOUtils.toString(streamCorrect, Charsets.UTF_8)
            assertEquals(referenceData, ical)
        }
    }

    @Test
    fun testFetchNetwork_success() {
        val etagCorrect = "33a64df551425fcc55e4d42a148795d9f25f89d4"
        val lastModifiedCorrect = "Wed, 21 Oct 2015 07:28:00 GMT"       // UNIX timestamp 1445405280
        val icalCorrect = testContext.resources.openRawResource(R.raw.vienna_evolution).use { streamCorrect ->
            IOUtils.toString(streamCorrect, Charsets.UTF_8)
        }

        // create mock response
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("ETag", etagCorrect)
            .addHeader("Last-Modified", lastModifiedCorrect)
            .setBody(icalCorrect))

        // make request to local mock server
        var ical: String? = null
        var etag: String? = null
        var lastmod: Long? = null
        val fetcher = object: CalendarFetcher(appContext, server.url("/").toAndroidUri()) {
            override suspend fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?, displayName: String?) {
                data.use {
                    ical = IOUtils.toString(it, Charsets.UTF_8)
                    etag = eTag
                    lastmod = lastModified
                }
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
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP)
            .addHeader("Location", server.url("new-location/vienna-evolution.ics")))
        // 2. redirect with relative target URL
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP)
            .addHeader("Location", "the-file-is-here"))
        // 3. finally the real resource
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("icalCorrect"))

        // make initial request to local mock server
        val baseUrl = server.url("/").toAndroidUri()
        var ical: String? = null
        val redirects = LinkedList<Uri>()
        val fetcher = object: CalendarFetcher(appContext, baseUrl) {
            override suspend fun onRedirect(httpCode: Int, target: Uri) {
                redirects += target
                super.onRedirect(httpCode, target)
            }
            override suspend fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?, displayName: String?) {
                data.use { ical = IOUtils.toString(it, Charsets.UTF_8) }
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

    @Test(expected = HttpInvalidResponseException::class)
    fun testFetchNetwork_onRedirectWithoutLocation() {
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))

        var e: Exception? = null
        runBlocking {
            object : CalendarFetcher(appContext, server.url("/").toAndroidUri()) {
                override suspend fun onError(error: Exception) {
                    e = error
                }
            }.fetch()
        }

        throw e!!
    }

    @Test
    fun testFetchNetwork_onNotModified() {
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_NOT_MODIFIED))

        var notModified = false
        runBlocking {
            object : CalendarFetcher(appContext, server.url("/").toAndroidUri()) {
                override suspend fun onNotModified() {
                    notModified = true
                }
            }.fetch()
        }

        assert(notModified)
    }

    @Test(expected = IOException::class)
    fun testFetchNetwork_onError() {
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_NOT_FOUND))

        var e: Exception? = null
        runBlocking {
            object : CalendarFetcher(appContext, server.url("/").toAndroidUri()) {
                override suspend fun onError(error: Exception) {
                    e = error
                }
            }.fetch()
        }

        throw e!!
    }
}