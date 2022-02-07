/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.net.Uri
import org.apache.commons.lang3.time.DateUtils
import org.junit.Assert.*
import org.junit.Test

class HttpUtilsTest {

    @Test
    fun testParseDate_invalidDate() {
        assertNull(HttpUtils.parseDate("invalid date"))
    }

    @Test
    fun testParseDate_someValidDate() {
        val testDateString = "Mon, 07 Feb 2022 23:59:59 UTC"
        val testDate = HttpUtils.parseDate(testDateString)
        val validDate = DateUtils.parseDate(testDateString,
            "EEE, dd MMM yyyy HH:mm:ss zzz", // RFC 822, updated by RFC 1123 with any TZ
        )
        assertEquals(testDate, validDate)
    }

    @Test
    fun testSupportsAuthentication() {
        assertEquals(true, HttpUtils.supportsAuthentication(Uri.parse("https://example.com")))
        assertEquals(true, HttpUtils.supportsAuthentication(Uri.parse("http://example.com")))
        assertEquals(false, HttpUtils.supportsAuthentication(Uri.parse("content://example.com")))
        assertEquals(false, HttpUtils.supportsAuthentication(Uri.parse("garbage://example.com")))
    }

}

