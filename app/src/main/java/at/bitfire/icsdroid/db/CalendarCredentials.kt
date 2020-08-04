/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.db

import android.annotation.SuppressLint
import android.content.Context

class CalendarCredentials(context: Context) {

    companion object {
        private const val PREF_CREDENTIALS = "basicAuth"
    }

    private val credentialPrefs = context.getSharedPreferences(PREF_CREDENTIALS, 0)


    @SuppressLint("ApplySharedPref")
    fun getCredentials(calendar: LocalCalendar): Pair<String?, String?> {
        val url = calendar.url!!

        var username: String? = null
        var password: String? = null

        username = username ?: credentialPrefs.getString("username_$url", null)
        password = password ?: credentialPrefs.getString("password_$url", null)

        return Pair(username, password)
    }

    fun putCredentials(calendar: LocalCalendar, username: String?, password: String?) {
        val prefs = credentialPrefs.edit()
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

}