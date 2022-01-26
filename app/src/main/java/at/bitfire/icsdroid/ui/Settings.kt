/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.content.Context
import android.content.SharedPreferences

class Settings(context: Context) {

    companion object {
        const val FORCE_DARK_MODE = "forceDarkMode"
    }


    private val prefs: SharedPreferences = context.getSharedPreferences("icsx5", 0)

    fun forceDarkMode(): Boolean =
            prefs.getBoolean(FORCE_DARK_MODE, false)

    fun forceDarkMode(force: Boolean) {
        prefs   .edit()
                .putBoolean(FORCE_DARK_MODE, force)
                .apply()
    }

}