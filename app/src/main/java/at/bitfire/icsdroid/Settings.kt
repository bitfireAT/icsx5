/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.lifecycle.LiveData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking

class Settings(context: Context) {

    companion object {
        @Deprecated("Use DataStore")
        const val FORCE_DARK_MODE = "forceDarkMode"

        val ForceDarkMode = booleanPreferencesKey("forceDarkMode")

        val PrefNextReminder = longPreferencesKey("nextDonationReminder")
    }

    private val dataStore = context.dataStore


    fun forceDarkModeFlow(): Flow<Boolean> = dataStore.data.map { it[ForceDarkMode] ?: false }

    suspend fun forceDarkMode(force: Boolean) {
        // save setting
        dataStore.edit { it[ForceDarkMode] = force }
    }

}
