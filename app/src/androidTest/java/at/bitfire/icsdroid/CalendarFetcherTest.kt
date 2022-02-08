/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.content.ContentProviderClient
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.FileProvider.getUriForFile
import androidx.test.platform.app.InstrumentationRegistry
import at.bitfire.ical4android.MiscUtils.ContentProviderClientHelper.closeCompat
import at.bitfire.icsdroid.Constants.TAG
import at.bitfire.icsdroid.HttpUtils.toUri
import okhttp3.MediaType
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.io.InputStream


class CalendarFetcherTest {

    companion object {

        private lateinit var context: Context
        private lateinit var provider: ContentProviderClient

        @BeforeClass
        @JvmStatic
        fun setUp() {
            context = InstrumentationRegistry.getInstrumentation().targetContext
            provider = context.contentResolver.acquireContentProviderClient(CalendarContract.AUTHORITY)!!
        }

        @AfterClass
        @JvmStatic
        fun disconnect() {
            provider.closeCompat()
        }

    }

    @Test
    fun testFetchLocal_readsCorrectly() {
        val uri = Uri.fromFile(File("vienna-evolution.ics"))
        Log.w(TAG, uri.toString())

        val fetcher = object: CalendarFetcher(context, uri) {
            override fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?, displayName: String?) {

                // check first few lines of stream equal actual file contents

            }
        }

        fetcher.fetchLocal()
    }

    @Test
    fun testFetchLocal_correctDisplayName() {

        val newFile = File("vienna-evolution.ics")
        val uri: Uri = getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", newFile);

        Log.w(TAG, uri.toString())

        val fetcher = object: CalendarFetcher(context, uri) {
            override fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?, displayName: String?) {
                Log.d(TAG, displayName.toString())
                assertEquals("vienna-evolution.ics", displayName)
            }
        }


        fetcher.run()
    }
}