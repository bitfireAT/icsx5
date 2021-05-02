package at.bitfire.icsdroid.ui

import android.content.Context

class Settings(context: Context) {

    companion object {
        val FORCE_DARK_MODE = "forceDarkMode"
    }


    val prefs = context.getSharedPreferences("icsx5", 0)

    fun forceDarkMode(): Boolean =
            prefs.getBoolean(FORCE_DARK_MODE, false)

    fun forceDarkMode(force: Boolean) {
        prefs   .edit()
                .putBoolean(FORCE_DARK_MODE, force)
                .apply()
    }

}