package at.bitfire.icsdroid

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import at.bitfire.icsdroid.ui.Settings

class MyApp: Application() {

    override fun onCreate() {
        super.onCreate()

        // dark mode is not persisted over app restarts
        if (Settings(this).forceDarkMode())
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

}