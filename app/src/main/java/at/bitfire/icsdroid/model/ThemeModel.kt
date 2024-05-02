package at.bitfire.icsdroid.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import at.bitfire.icsdroid.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class ThemeModel(application: Application) : AndroidViewModel(application) {
    private val settings = Settings(application)

    val forceDarkMode = settings.forceDarkModeFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
}
