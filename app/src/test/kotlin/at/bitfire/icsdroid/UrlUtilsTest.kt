package at.bitfire.icsdroid

import at.bitfire.icsdroid.UriUtils.stripUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class UrlUtilsTest {
    @Test
    fun testStripUrl() {
        val url = "This is a URL: https://example.com/more/and/more?query=true.&param=test.more"
        val strippedUrl = url.stripUrl()
        assertEquals("https://example.com/more/and/more?query=true_&param=test.more", strippedUrl)
    }
}
