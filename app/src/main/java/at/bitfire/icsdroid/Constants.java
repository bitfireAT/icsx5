/*
 * Copyright (c) 2013 – 2015 Ricki Hirner (bitfire web engineering).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 */

package at.bitfire.icsdroid;

import android.net.Uri;
import android.os.Build;

import net.fortuna.ical4j.util.CompatibilityHints;

public class Constants {

    static {
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_UNFOLDING, true);
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true);
    }

    public static final String USER_AGENT = "ICSdroid/" + BuildConfig.VERSION_NAME + " (Android/" + Build.VERSION.RELEASE + ")";

    public static final Uri donationUri = Uri.parse("https://icsdroid.bitfire.at/donate?pk_campaign=icsdroid-app");

}
