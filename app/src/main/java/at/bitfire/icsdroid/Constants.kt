/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid

import android.net.Uri
import android.os.Build

object Constants {

    const val TAG = "icsdroid"

    val USER_AGENT = "ICSdroid/" + BuildConfig.VERSION_NAME + " (Android/" + Build.VERSION.RELEASE + ")"
    const val MAX_REDIRECTS = 5

    val donationUri = Uri.parse("https://icsdroid.bitfire.at/donate/?pk_campaign=icsdroid-app")!!

}
