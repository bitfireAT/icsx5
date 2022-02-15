/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider.getUriForFile
import androidx.test.platform.app.InstrumentationRegistry
import at.bitfire.icsdroid.Constants.TAG
import okhttp3.MediaType
import org.junit.*
import java.io.*


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

//    @Test
//    fun testFetchLocal_readsCorrectly() {
//        val uri = Uri.fromFile(File("sampledata/vienna-evolution.ics"))
//        Log.w(TAG, uri.toString())
//
//        val fetcher = object: CalendarFetcher(appContext, uri) {
//            override fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?, displayName: String?) {
//
//                // check first few lines of stream equal actual file contents
//
//            }
//        }
//
//        fetcher.fetchLocal()
//    }

    @Test
    fun testFetchLocal_correctDisplayName() {

        val inputStream = testContext.assets.open("vienna-evolution.ics")

        val file = File(appContext.filesDir, "vienna-evolution.ics")
        val outputStream = FileOutputStream(file)
        val uri: Uri = getUriForFile(appContext, BuildConfig.APPLICATION_ID + ".test.provider", file);
        inputStream.copyTo(outputStream, 4096)

        Log.v(TAG, file.absolutePath)
        Log.w(TAG, uri.toString())

//        // assert display name matches
//        object: CalendarFetcher(context, uri) {
//            override fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?, displayName: String?) {
//                Log.d(TAG, displayName.toString())
//                assertEquals("vienna-evolution.ics", displayName)
//            }
//        }.run()
    }

    private fun InputStream.getFilePath(dir: File, name: String, extension: String): String {
        val file = File(dir, "$name.$extension")
        val outputStream = FileOutputStream(file)
        this.copyTo(outputStream, 4096)
        return file.absolutePath
    }

}