/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.os.Build
import at.bitfire.ical4android.ical4jVersion
import okhttp3.OkHttp

object Constants {

    const val TAG = "icsx5"

    val USER_AGENT = "ICSx5/${BuildConfig.VERSION_NAME} (ical4j/${ical4jVersion} okhttp/${OkHttp.VERSION} Android/${Build.VERSION.RELEASE})"

}
