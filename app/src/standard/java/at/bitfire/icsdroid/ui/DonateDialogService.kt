/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.edit
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.Settings.Companion.nextReminder
import at.bitfire.icsdroid.dataStore
import at.bitfire.icsdroid.service.ComposableStartupService
import at.bitfire.icsdroid.service.ComposableStartupService.Companion.FLAG_DONATION_DIALOG
import at.bitfire.icsdroid.ui.partials.GenericAlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DonateDialogService: ComposableStartupService {
    companion object {

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

    override val flags: Int = FLAG_DONATION_DIALOG

    @Composable
    private fun getActivity(): AppCompatActivity? = LocalContext.current as? AppCompatActivity

    /**
     * Whether [Content] should be displayed or not.
     *
     * Observes the value of the preference with key [nextReminder] and sets its value to
     * *true* if the preference value lies in the past, or *false* otherwise.
     */
    @Composable
    override fun shouldShow(): State<Boolean> {
        val context = LocalContext.current
        val dataStore = context.dataStore
        val flow = remember(dataStore) {
            dataStore.data.map { (it[nextReminder] ?: 0) < System.currentTimeMillis() }
        }
        return flow.collectAsState(initial = false)
    }

    /**
     * Dismisses the dialog for the given amount of milliseconds by updating the preference.
     */
    private suspend fun dismissDialogForMillis(activity: AppCompatActivity, millis: Long) =
        activity.dataStore.edit { it[nextReminder] = System.currentTimeMillis() + millis }

    @Composable
    override fun Content() {
        val activity = getActivity()
        val uriHandler = LocalUriHandler.current
        GenericAlertDialog(
            onDismissRequest = { /* Cannot be dismissed */ },
            title = stringResource(R.string.donate_title),
            content = { Text(stringResource(R.string.donate_message)) },
            confirmButton = Pair(stringResource(R.string.donate_now).uppercase()) {
                CoroutineScope(Dispatchers.IO).launch {
                    dismissDialogForMillis(activity!!, SHOW_EVERY_MILLIS_DONATE)
                }
                uriHandler.openUri(DONATION_URI)
            },
            dismissButton = Pair(stringResource(R.string.donate_later).uppercase()) {
                CoroutineScope(Dispatchers.IO).launch {
                    dismissDialogForMillis(activity!!, SHOW_EVERY_MILLIS_DISMISS)
                }
            }
        )
    }
}
