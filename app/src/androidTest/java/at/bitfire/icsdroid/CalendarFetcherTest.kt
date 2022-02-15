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
import org.apache.commons.io.IOUtils
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import java.io.InputStream

class CalendarFetcherTest {

    companion object {

        private lateinit var appContext: Context
        private lateinit var testContext: Context

        @BeforeClass
        @JvmStatic
        fun setUp() {
            appContext = InstrumentationRegistry.getInstrumentation().targetContext
            testContext = InstrumentationRegistry.getInstrumentation().context
        }

    }

    @Test
    fun testFetchLocal_readsCorrectly() {
        val uri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${BuildConfig.APPLICATION_ID}/${R.raw.vienna_evolution}")

        var ical: String? = null
        val fetcher = object: CalendarFetcher(appContext, uri) {
            override fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?, displayName: String?) {
                // check first few lines of stream equal actual file contents
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
}