/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.db

import android.content.Context
import at.bitfire.icsdroid.db.entity.Subscription

class CalendarCredentials(context: Context) {

    companion object {
        private const val PREF_CREDENTIALS = "basicAuth"

        private const val KEY_PREFIX_USERNAME = "username_"
        private const val KEY_PREFIX_PASSWORD = "password_"
    }

    private val credentialPrefs = context.getSharedPreferences(PREF_CREDENTIALS, 0)

    @Deprecated("Use Room database", replaceWith = ReplaceWith("this.get(subscription)"), level = DeprecationLevel.ERROR)
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

    fun get(subscription: Subscription): Pair<String?, String?> {
        val url = subscription.url
        val id = subscription.id

        val username: String? =
            credentialPrefs.getString("$KEY_PREFIX_USERNAME$url", null) ?:  // legacy
            credentialPrefs.getString("$KEY_PREFIX_USERNAME$id", null)

        val password: String? =
            credentialPrefs.getString("$KEY_PREFIX_PASSWORD$url", null) ?:  // legacy
            credentialPrefs.getString("$KEY_PREFIX_PASSWORD$id", null)

        return Pair(username, password)
    }

    @Deprecated("Use Room database", replaceWith = ReplaceWith("this.put(subscription, username, password)"), level = DeprecationLevel.ERROR)
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

    fun put(subscription: Subscription, username: String?, password: String?) {
        val prefs = credentialPrefs.edit()
        val id = subscription.id

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