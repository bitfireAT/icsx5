/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.service.ComposableStartupService
import at.bitfire.icsdroid.ui.partials.GenericAlertDialog

class DonateDialogService: ComposableStartupService {
    companion object {
        const val PREF_NEXT_REMINDER = "nextDonationReminder"

        const val DONATION_URI = "https://icsx5.bitfire.at/donate/?pk_campaign=icsx5-app"

        /**
         * The amount of milliseconds in a day.
         */
        private const val ONE_DAY_MILLIS = 1000L * 60 * 60 * 24

        /**
         * When the donate button is clicked, the donation link will be opened. The dialog will be
         * shown again after this amount of ms.
         *
         * Default: 60 days
         */
        const val SHOW_EVERY_MILLIS_DONATE = ONE_DAY_MILLIS * 60

        /**
         * When the dismiss button is clicked, the donation dialog will be dismissed. It will be
         * shown again after this amount of ms.
         *
         * Default: 14 days
         */
        const val SHOW_EVERY_MILLIS_DISMISS = ONE_DAY_MILLIS * 14
    }

    private var preferences: SharedPreferences? = null

    override fun initialize(activity: AppCompatActivity) {
        if (preferences != null) return
        preferences = getPreferences(activity)
    }

    private fun getPreferences(activity: AppCompatActivity): SharedPreferences =
        preferences ?: activity.getPreferences(0).also { preferences = it }

    @Composable
    private fun getActivity(): AppCompatActivity? = LocalContext.current as? AppCompatActivity

    /**
     * Whether [Content] should be displayed or not.
     *
     * Observes the value of the preference with key [PREF_NEXT_REMINDER] and sets its value to
     * *true* if the preference value lies in the past, or *false* otherwise.
     */
    @Composable
    override fun shouldShow(): LiveData<Boolean> = remember { MutableLiveData(false) }.also {
        val activity = getActivity() ?: return@also
        DisposableEffect(it) {
            val listener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
                // Receive updates only for PREF_NEXT_REMINDER
                if (key != PREF_NEXT_REMINDER) return@OnSharedPreferenceChangeListener
                // Get preference value, calculate livedata value and post it
                val nextReminderTime = sharedPreferences.getLong(PREF_NEXT_REMINDER, 0)
                it.postValue(nextReminderTime < System.currentTimeMillis())
            }
            val preferences = getPreferences(activity)
            preferences.registerOnSharedPreferenceChangeListener(listener)
            listener.onSharedPreferenceChanged(preferences, PREF_NEXT_REMINDER)

            onDispose {
                preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }
    }

    /**
     * Dismisses the dialog for the given amount of milliseconds by updating the preference.
     */
    private fun dismissDialogForMillis(activity: AppCompatActivity, millis: Long) =
        getPreferences(activity)
            .edit()
            .putLong(PREF_NEXT_REMINDER, System.currentTimeMillis() + millis)
            .apply()

    @Composable
    override fun Content() {
        val activity = getActivity()
        val uriHandler = LocalUriHandler.current
        GenericAlertDialog(
            onDismissRequest = { /* Cannot be dismissed */ },
            title = stringResource(R.string.donate_title),
            content = { Text(stringResource(R.string.donate_message)) },
            confirmButton = Pair(stringResource(R.string.donate_now).uppercase()) {
                dismissDialogForMillis(activity!!, SHOW_EVERY_MILLIS_DONATE)
                uriHandler.openUri(DONATION_URI)
            },
            dismissButton = Pair(stringResource(R.string.donate_later).uppercase()) {
                dismissDialogForMillis(activity!!, SHOW_EVERY_MILLIS_DISMISS)
            }
        )
    }
}
