package at.bitfire.icsdroid

import android.util.Log
import org.apache.commons.lang3.time.DateUtils
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object HttpUtils {

    private const val httpDateFormatStr = "EEE, dd MMM yyyy HH:mm:ss 'GMT'"
    private val httpDateFormat = SimpleDateFormat(httpDateFormatStr, Locale.US)


    /**
     * Formats a date for use in HTTP headers using [httpDateFormat].
     *
     * @param date date to be formatted
     * @return date in HTTP-date format
     */
    fun formatDate(date: Date): String = httpDateFormat.format(date)


    /**
     * Parses a HTTP-date.
     *
     * @param dateStr date with format specified by RFC 7231 section 7.1.1.1
     * or in one of the obsolete formats (copied from okhttp internal date-parsing class)
     *
     * @return date, or null if date could not be parsed
     */
    fun parseDate(dateStr: String) = try {
        DateUtils.parseDate(dateStr,
                httpDateFormatStr,
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
        null
    }

}