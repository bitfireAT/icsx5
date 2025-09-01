/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid

import android.os.Build
import at.bitfire.ical4android.ical4jVersion
import okhttp3.OkHttp

object Constants {

    const val TAG = "icsx5"

    val USER_AGENT = "ICSx5/${BuildConfig.VERSION_NAME} (ical4j/${ical4jVersion} okhttp/${OkHttp.VERSION} Android/${Build.VERSION.RELEASE})"

    /**
     * Suggested user agents to allow compatibility with user agent blocking servers
     */
    val COMPATIBILITY_USER_AGENTS = mapOf(
        "Chrome Android" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.3",
        "Firefox Android" to "Mozilla/5.0 (Android 16; Mobile; rv:142.0) Gecko/142.0 Firefox/142.0",
        "Safari iOS" to "Mozilla/5.0 (iPhone; CPU iPhone OS 18_3_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.3.1 Mobile/15E148 Safari/604."
    )

}
