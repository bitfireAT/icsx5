/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class Settings(context: Context) {

    companion object {
        const val FORCE_DARK_MODE = "forceDarkMode"
    }


    private val prefs: SharedPreferences = context.getSharedPreferences("icsx5", 0)

    fun forceDarkMode(): Boolean = prefs.getBoolean(FORCE_DARK_MODE, false)

    fun forceDarkMode(force: Boolean) {
        prefs.edit()
            .putBoolean(FORCE_DARK_MODE, force)
            .apply()
    }

    /**
     * Provides the state of the Force Dark mode preference that gets updated synchronously. This
     * can be useful for keeping the UI updated with the current value.
     */
    @Composable
    fun ForceDarkModeSync(onChange: (value: Boolean) -> Unit) {
        val eventHandler = rememberUpdatedState(onChange)
        val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

        DisposableEffect(lifecycleOwner.value) {
            val lifecycle = lifecycleOwner.value.lifecycle

            val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                if (key != FORCE_DARK_MODE) return@OnSharedPreferenceChangeListener
                val value = prefs.getBoolean(key, false)
                eventHandler.value(value)
            }

            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        prefs.registerOnSharedPreferenceChangeListener(listener)

                        // The first time the listener is added, manually send an update
                        listener.onSharedPreferenceChanged(prefs, FORCE_DARK_MODE)
                    }

                    Lifecycle.Event.ON_PAUSE -> {
                        prefs.unregisterOnSharedPreferenceChangeListener(listener)
                    }

                    else -> {}
                }
            }

            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
            }
        }
    }

}