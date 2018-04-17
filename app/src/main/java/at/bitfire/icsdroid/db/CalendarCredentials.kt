/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.db

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context

object CalendarCredentials {

    private const val PREF_CREDENTIALS = "basicAuth"

    @SuppressLint("ApplySharedPref")
    fun getCredentials(context: Context, calendar: LocalCalendar): Pair<String?, String?> {
        val prefs = context.getSharedPreferences(PREF_CREDENTIALS, 0)
        val url = calendar.url!!

        var username: String? = null
        var password: String? = null

        calendar.legacyUsername?.let {
            username = it
            prefs   .edit()
                    .putString("username_$url", username)
                    .commit()
            removeLegacyField(calendar, LocalCalendar.COLUMN_USERNAME)
        }
        calendar.legacyPassword?.let {
            password = it
            prefs   .edit()
                    .putString("password_$url", password)
                    .commit()
            removeLegacyField(calendar, LocalCalendar.COLUMN_PASSWORD)
        }

        username = username ?: prefs.getString("username_$url", null)
        password = password ?: prefs.getString("password_$url", null)

        return Pair(username, password)
    }

    fun putCredentials(context: Context, calendar: LocalCalendar, username: String?, password: String?) {
        val prefs = context.getSharedPreferences(PREF_CREDENTIALS, 0).edit()
        val url = calendar.url!!

        if (username != null)
            prefs.putString("username_$url", username)
        else
            prefs.remove("username_$url")

        if (password != null)
            prefs.putString("password_$url", password)
        else
            prefs.remove("password_$url")

        prefs.apply()
    }


    private fun removeLegacyField(calendar: LocalCalendar, field: String) {
        val values = ContentValues(1)
        values.putNull(field)
        calendar.provider.update(calendar.calendarSyncURI(), values, null, null)
    }

}