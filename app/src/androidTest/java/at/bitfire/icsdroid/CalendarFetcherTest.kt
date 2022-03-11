/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.content.ContentResolver
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import at.bitfire.icsdroid.HttpUtils.toAndroidUri
import at.bitfire.icsdroid.test.BuildConfig
import at.bitfire.icsdroid.test.R
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

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext!!
        val testContext = InstrumentationRegistry.getInstrumentation().context!!

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
            override fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?, displayName: String?) {
                ical = IOUtils.toString(data, Charsets.UTF_8)
                data.close()
            }
        }
        fetcher.run()

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
            override fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?, displayName: String?) {
                ical = IOUtils.toString(data, Charsets.UTF_8)
                etag = eTag
                lastmod = lastModified
                data.close()
            }
        }
        fetcher.run()

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
            override fun onRedirect(httpCode: Int, target: Uri) {
                redirects += target
                super.onRedirect(httpCode, target)
            }
            override fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?, displayName: String?) {
                ical = IOUtils.toString(data, Charsets.UTF_8)
                data.close()
            }
        }
        fetcher.run()

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
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))

        var e: Exception? = null
        object: CalendarFetcher(appContext, server.url("/").toAndroidUri()) {
            override fun onError(error: Exception) {
                e = error
            }
        }.run()

        assertEquals(IOException::class.java, e?.javaClass)
    }

    @Test
    fun testFetchNetwork_onNotModified() {
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_NOT_MODIFIED))

        var notModified = false
        object: CalendarFetcher(appContext, server.url("/").toAndroidUri()) {
            override fun onNotModified() {
                notModified = true
            }
        }.run()

        assert(notModified)
    }

    @Test
    fun testFetchNetwork_onError() {
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_NOT_FOUND))

        var e: Exception? = null
        object: CalendarFetcher(appContext, server.url("/").toAndroidUri()) {
            override fun onError(error: Exception) {
                e = error
            }
        }.run()

        assertEquals(IOException::class.java, e?.javaClass)
    }
}