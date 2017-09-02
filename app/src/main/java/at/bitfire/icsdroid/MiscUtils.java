/*
 * Copyright (c) 2013 – 2017 Ricki Hirner (bitfire web engineering).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 *
 */

package at.bitfire.icsdroid;

import android.util.Log;

import org.apache.commons.codec.Charsets;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiscUtils {

    private static final Pattern regexContentTypeCharset = Pattern.compile("[; ]\\s*charset=\"?([^\"]+)\"?", Pattern.CASE_INSENSITIVE);

    public static Charset charsetFromContentType(String contentType) {
        // assume UTF-8 by default [RFC 5445 3.1.4]
        Charset charset = Charsets.UTF_8;

        if (contentType != null) {
            Matcher m = regexContentTypeCharset.matcher(contentType);
            if (m.find())
                try {
                    charset = Charset.forName(m.group(1));
                    Log.v(Constants.TAG, "Using charset " + charset.displayName());
                } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
                    Log.e(Constants.TAG, "Illegal or unsupported character set, assuming UTF-8", e);
                }
        }

        return charset;
    }

}
