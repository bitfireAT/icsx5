/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid

import android.net.Uri;
import android.os.Build;

import net.fortuna.ical4j.util.CompatibilityHints;

object Constants {

    init {
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_UNFOLDING, true)
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true)
    }

    @JvmField val TAG = "icsdroid"

    @JvmField val USER_AGENT = "ICSdroid/" + BuildConfig.VERSION_NAME + " (Android/" + Build.VERSION.RELEASE + ")"
    @JvmField val MAX_REDIRECTS = 5

    @JvmField val donationUri = Uri.parse("https://icsdroid.bitfire.at/donate/?pk_campaign=icsdroid-app")!!

}
