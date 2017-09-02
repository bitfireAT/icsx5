/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid

import android.util.Log
import org.apache.commons.codec.Charsets
import java.nio.charset.Charset
import java.util.regex.Pattern

object MiscUtils {

    private val regexContentTypeCharset = Pattern.compile("[; ]\\s*charset=\"?([^\"]+)\"?", Pattern.CASE_INSENSITIVE)

    @JvmStatic
    fun charsetFromContentType(contentType: String?): Charset {
        // assume UTF-8 by default [RFC 5445 3.1.4]
        var charset = Charsets.UTF_8

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

}
