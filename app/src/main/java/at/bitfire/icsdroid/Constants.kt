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
import at.bitfire.ical4android.Constants

object Constants {

    const val TAG = "icsx5"

    val USER_AGENT = "ICSx5/" + BuildConfig.VERSION_NAME + " (ical4j/" + Constants.ical4jVersion + " Android/" + Build.VERSION.RELEASE + ")"
    const val MAX_REDIRECTS = 5

    val donationUri = Uri.parse("https://icsx5.bitfire.at/donate/?pk_campaign=icsx5-app")!!

}
