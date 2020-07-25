/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid

import android.os.Build
import at.bitfire.ical4android.Ical4Android
import okhttp3.OkHttp

object Constants {

    const val TAG = "icsx5"

    val USER_AGENT = "ICSx5/${BuildConfig.VERSION_NAME} (ical4j/${Ical4Android.ical4jVersion} okhttp/${OkHttp.VERSION} Android/${Build.VERSION.RELEASE})"

}
