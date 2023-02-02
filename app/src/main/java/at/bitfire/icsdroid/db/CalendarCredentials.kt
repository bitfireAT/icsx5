/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.db

import android.content.Context

@Deprecated(
    "Use Room's Credentials from database.",
    replaceWith = ReplaceWith(
        "CredentialsDao.getInstance(context)",
        "at.bitfire.icsdroid.db.AppDatabase"
    ),
)
class CalendarCredentials(context: Context) {

    companion object {
        private const val PREF_CREDENTIALS = "basicAuth"

        private const val KEY_PREFIX_USERNAME = "username_"
        private const val KEY_PREFIX_PASSWORD = "password_"
    }

    private val credentialPrefs = context.getSharedPreferences(PREF_CREDENTIALS, 0)


    fun get(calendar: LocalCalendar): Pair<String?, String?> {
        val url = calendar.url
        val id = calendar.id

        val username: String? =
                credentialPrefs.getString("$KEY_PREFIX_USERNAME$url", null) ?:  // legacy
                credentialPrefs.getString("$KEY_PREFIX_USERNAME$id", null)

        val password: String? =
                credentialPrefs.getString("$KEY_PREFIX_PASSWORD$url", null) ?:  // legacy
                credentialPrefs.getString("$KEY_PREFIX_PASSWORD$id", null)

        return Pair(username, password)
    }

    fun put(calendar: LocalCalendar, username: String?, password: String?) {
        val prefs = credentialPrefs.edit()
        val id = calendar.id

        if (username != null)
            prefs.putString("$KEY_PREFIX_USERNAME$id", username)
        else
            prefs.remove("$KEY_PREFIX_USERNAME$id")

        if (password != null)
            prefs.putString("$KEY_PREFIX_PASSWORD$id", password)
        else
            prefs.remove("$KEY_PREFIX_PASSWORD$id")

        prefs.apply()
    }

}