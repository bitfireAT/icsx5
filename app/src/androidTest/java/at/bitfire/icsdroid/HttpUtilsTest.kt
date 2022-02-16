/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HttpUtilsTest {

    @Test
    fun testParseDate_invalidDate() {
        assertNull(HttpUtils.parseDate("invalid date"))
    }

    @Test
    fun testParseDate_someValidDate_GMT() {
        val testDateString = "Wed, 21 Oct 2015 07:28:00 GMT"
        val testDate = HttpUtils.parseDate(testDateString)
        assertEquals(1445412480000L, testDate?.time)
    }

    @Test
    fun testParseDate_someValidDate_UTC() {
        val testDateString = "Mon, 07 Feb 2022 23:59:59 UTC"
        val testDate = HttpUtils.parseDate(testDateString)
        assertEquals(1644278399000L, testDate?.time)
    }

    @Test
    fun testSupportsAuthentication() {
        assertEquals(true, HttpUtils.supportsAuthentication(Uri.parse("https://example.com")))
        assertEquals(true, HttpUtils.supportsAuthentication(Uri.parse("http://example.com")))
        assertEquals(false, HttpUtils.supportsAuthentication(Uri.parse("content://example.com")))
        assertEquals(false, HttpUtils.supportsAuthentication(Uri.parse("garbage://example.com")))
    }

}

