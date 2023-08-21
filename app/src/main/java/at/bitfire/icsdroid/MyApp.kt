/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class MyApp: Application() {

    companion object {

        fun setNightMode(forceDarkMode: Boolean) {
            AppCompatDelegate.setDefaultNightMode(
                if (forceDarkMode)
                    AppCompatDelegate.MODE_NIGHT_YES
                else
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            )
        }

    }


    override fun onCreate() {
        super.onCreate()

        // dark mode is not persisted over app restarts
        setNightMode(Settings(this).forceDarkMode())
    }

}