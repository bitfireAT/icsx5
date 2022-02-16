/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import at.bitfire.icsdroid.test.BuildConfig
import at.bitfire.icsdroid.test.R
import okhttp3.MediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.apache.commons.io.IOUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import java.io.IOException
import java.io.InputStream
import kotlin.Exception


class CalendarFetcherTest {

    companion object {

        private lateinit var appContext: Context
        private lateinit var testContext: Context
        lateinit var server: MockWebServer

        @BeforeClass
        @JvmStatic
        fun setUp() {
            appContext = InstrumentationRegistry.getInstrumentation().targetContext
            testContext = InstrumentationRegistry.getInstrumentation().context

            server = MockWebServer()
            server.start(3000)
        }

        @After
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
        val lastModifiedCorrect = "Wed, 21 Oct 2015 07:28:00 GMT"
        val icalCorrect = testContext.resources.openRawResource(R.raw.vienna_evolution).use { streamCorrect ->
            IOUtils.toString(streamCorrect, Charsets.UTF_8)
        }

        // create mock response
        server.enqueue(MockResponse()
            .setResponseCode(200)
            .addHeader("ETag", etagCorrect)
            .addHeader("Last-Modified", lastModifiedCorrect)
            .setBody(icalCorrect))


        // make request to local mock server
        val baseUrl = server.url("/")
        val uri = Uri.parse(baseUrl.toString() + "vienna-evolution.ics")
        var ical: String? = null
        var etag: String? = null
        var lastmod: Long? = null
        val fetcher = object: CalendarFetcher(appContext, uri) {
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
        assertEquals(HttpUtils.parseDate(lastModifiedCorrect)?.time, lastmod)
        assertEquals(icalCorrect, ical)
    }

    @Test
    fun testFetchNetwork_onRedirectWithLocation() {

        // create mock responses
        server.enqueue(MockResponse()
            .setResponseCode(300)
            .addHeader("Location", server.url("new-location/vienna-evolution.ics")))
        server.enqueue(MockResponse()
            .setResponseCode(300)
            .addHeader("Location", server.url("the-file-is-here")))
        server.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("icalCorrect"))

        // make initial request to local mock server
        val baseUrl = server.url("/")
        val uri = Uri.parse(baseUrl.toString() + "vienna-evolution.ics")
        var ical: String? = null
        val redirects = arrayListOf<Uri>()
        val fetcher = object: CalendarFetcher(appContext, uri) {
            override fun onRedirect(httpCode: Int, target: Uri) {
                redirects.add(target)
                super.onRedirect(httpCode, target)
            }
            override fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?, displayName: String?) {
                ical = IOUtils.toString(data, Charsets.UTF_8)
                data.close()
            }
        }
        fetcher.run()

        // assert redirects are made correctly
        assertEquals(arrayListOf(
            Uri.parse(baseUrl.toString() + "new-location/vienna-evolution.ics"),
            Uri.parse(baseUrl.toString() + "the-file-is-here")
        ), redirects)

        // assert onSuccess is called
        assertEquals("icalCorrect", ical)
    }

    @Test
    fun testFetchNetwork_onRedirectWithoutLocation() {

        server.enqueue(MockResponse()
            .setResponseCode(300))

        val baseUrl = server.url("/")
        val uri = Uri.parse(baseUrl.toString() + "vienna-evolution.ics")

        var e:Exception = IOException()
        object: CalendarFetcher(appContext, uri){
            override fun onError(error: Exception) {
                e = error
            }
        }.run()

        assertEquals(IOException::class.java, e::class.java)
    }

    @Test
    fun testFetchNetwork_onNotModified() {

        server.enqueue(MockResponse()
            .setResponseCode(304))

        val baseUrl = server.url("/")
        val uri = Uri.parse(baseUrl.toString() + "vienna-evolution.ics")

        var notModified = false
        object: CalendarFetcher(appContext, uri){
            override fun onNotModified() {
                notModified = true
            }
        }.run()

        assert(notModified)
    }

    @Test
    fun testFetchNetwork_onError() {

        server.enqueue(MockResponse()
            .setResponseCode(404))

        val baseUrl = server.url("/")
        val uri = Uri.parse(baseUrl.toString() + "vienna-evolution.ics")

        var e:Exception = IOException()
        object: CalendarFetcher(appContext, uri){
            override fun onError(error: Exception) {
                e = error
            }
        }.run()

        assertEquals(IOException::class.java, e::class.java)
    }
}