/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid

import android.net.Uri
import android.util.Log
import okhttp3.HttpUrl
import org.apache.commons.lang3.time.DateUtils
import org.apache.commons.lang3.time.TimeZones
import java.net.URI
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object HttpUtils {

    const val HTTP_PERMANENT_REDIRECT = 308

    private const val httpDateFormatStr = "EEE, dd MMM yyyy HH:mm:ss 'GMT'"
    private val httpDateFormat = SimpleDateFormat(httpDateFormatStr, Locale.US).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone(TimeZones.GMT_ID)
    }


    /**
     * Formats a date for use in HTTP headers using [httpDateFormat].
     *
     * See also https://github.com/square/okhttp/blob/master/okhttp/src/jvmMain/kotlin/okhttp3/internal/http/dates.kt
     *
     * @param date date to be formatted
     * @return date in HTTP-date format
     */
    fun formatDate(date: Date): String = httpDateFormat.format(date)


    /**
     * Parses a HTTP-date.
     *
     * See also https://github.com/square/okhttp/blob/master/okhttp/src/jvmMain/kotlin/okhttp3/internal/http/dates.kt
     *
     * @param dateStr date with format specified by RFC 7231 section 7.1.1.1
     * or in one of the obsolete formats (copied from okhttp internal date-parsing class)
     *
     * @return date, or null if date could not be parsed
     */
    fun parseDate(dateStr: String): Date? {
        try {
            try {
                return httpDateFormat.parse(dateStr)
            } catch (ignored: ParseException) {
                // not in httpDateFormat, try other formats
            }

            return DateUtils.parseDate(
                dateStr,
                "EEE, dd MMM yyyy HH:mm:ss zzz", // RFC 822, updated by RFC 1123 with any TZ
                "EEEE, dd-MMM-yy HH:mm:ss zzz", // RFC 850, obsoleted by RFC 1036 with any TZ.
                "EEE MMM d HH:mm:ss yyyy", // ANSI C's asctime() format
                // Alternative formats.
                "EEE, dd-MMM-yyyy HH:mm:ss z",
                "EEE, dd-MMM-yyyy HH-mm-ss z",
                "EEE, dd MMM yy HH:mm:ss z",
                "EEE dd-MMM-yyyy HH:mm:ss z",
                "EEE dd MMM yyyy HH:mm:ss z",
                "EEE dd-MMM-yyyy HH-mm-ss z",
                "EEE dd-MMM-yy HH:mm:ss z",
                "EEE dd MMM yy HH:mm:ss z",
                "EEE,dd-MMM-yy HH:mm:ss z",
                "EEE,dd-MMM-yyyy HH:mm:ss z",
                "EEE, dd-MM-yyyy HH:mm:ss z",
                /* RI bug 6641315 claims a cookie of this format was once served by www.yahoo.com */
                "EEE MMM d yyyy HH:mm:ss z"
            )
        } catch (e: ParseException) {
            Log.w(Constants.TAG, "Couldn't parse date: $dateStr, ignoring")
        }
        return null
    }

    /**
     * Whether given URL has an accepted protocol.
     *
     * Currently only HTTP and HTTPS URIs are accepted.
     *
     * @return true if URI contains valid scheme; false if it does not
     */
    fun acceptedProtocol(uri: Uri) =
        when (uri.scheme?.lowercase()) {
            "http", "https" -> true
            else -> false
        }


    fun HttpUrl.toAndroidUri(): Uri = Uri.parse(toString())

    fun Uri.toURI(): URI = URI(toString())
    fun URI.toUri(): Uri = Uri.parse(toString())

}