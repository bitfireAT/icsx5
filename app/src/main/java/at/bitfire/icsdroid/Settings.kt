/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class Settings(context: Context) {

    companion object {
        @Deprecated("Use DataStore")
        const val FORCE_DARK_MODE = "forceDarkMode"

        val forceDarkMode = booleanPreferencesKey("forceDarkMode")

        val nextReminder = longPreferencesKey("nextDonationReminder")
    }

    private val dataStore = context.dataStore


    fun forceDarkModeFlow(): Flow<Boolean> = dataStore.data.map { it[forceDarkMode] ?: false }

    suspend fun forceDarkMode(force: Boolean) {
        // save setting
        dataStore.edit { it[forceDarkMode] = force }
    }

}
