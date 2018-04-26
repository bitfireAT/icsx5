/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

object MiscUtils {

    private val regexContentTypeCharset = Pattern.compile("[; ]\\s*charset=\"?([^\"]+)\"?", Pattern.CASE_INSENSITIVE)

    fun charsetFromContentType(contentType: String?): Charset {
        // assume UTF-8 by default [RFC 5445 3.1.4]
        var charset = StandardCharsets.UTF_8

        contentType?.let {
            val m = regexContentTypeCharset.matcher(it)
            if (m.find())
                try {
                    charset = Charset.forName(m.group(1))
                    Log.v(Constants.TAG, "Using charset ${charset.displayName()}")
                } catch(e: Exception) {
                    Log.e(Constants.TAG, "Illegal or unsupported character set, assuming UTF-8", e)
                }
        }

        return charset
    }

    /**
     * Opens a connection from an URL and prepares some settings like timoues and
     * request headers (User-Agent, Accept, etc.).
     */
    fun prepareConnection(url: URL): URLConnection {
        val conn = url.openConnection()
        conn.connectTimeout = 7000
        conn.readTimeout = 20000

        if (conn is HttpURLConnection) {
            conn.setRequestProperty("User-Agent", Constants.USER_AGENT)
            conn.setRequestProperty("Accept", "text/calendar")
        }

        return conn
    }

}
