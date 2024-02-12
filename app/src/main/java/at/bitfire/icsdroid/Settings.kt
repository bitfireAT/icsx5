/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData

class Settings(context: Context) {

    companion object {
        private const val FORCE_DARK_MODE = "forceDarkMode"
    }


    private val prefs: SharedPreferences = context.getSharedPreferences("icsx5", 0)

    fun forceDarkMode(): Boolean = prefs.getBoolean(FORCE_DARK_MODE, false)

    fun forceDarkModeLive(): LiveData<Boolean> = object: LiveData<Boolean>() {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == FORCE_DARK_MODE) {
                val forceDarkMode = prefs.getBoolean(key, false)
                postValue(forceDarkMode)
            }
        }

        override fun onActive() {
            prefs.registerOnSharedPreferenceChangeListener(listener)
            listener.onSharedPreferenceChanged(prefs, FORCE_DARK_MODE)
        }

        override fun onInactive() {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    fun forceDarkMode(force: Boolean) {
        // save setting
        prefs.edit()
            .putBoolean(FORCE_DARK_MODE, force)
            .apply()
    }

}