/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui.views

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.ui.screen.EditCalendarScreen
import at.bitfire.icsdroid.ui.theme.setContentThemed

class EditCalendarActivity: AppCompatActivity() {

    companion object {
        // Used by intents only
        const val EXTRA_SUBSCRIPTION_ID = "subscriptionId"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentThemed {
            EditCalendarScreen(
                application,
                subscriptionId = intent.getLongExtra(EXTRA_SUBSCRIPTION_ID, -1),
                { onShare(it) },
                { finish() }
            )
        }
    }

    private fun onShare(subscription: Subscription) =
        ShareCompat.IntentBuilder(this)
            .setSubject(subscription.displayName)
            .setText(subscription.url.toString())
            .setType("text/plain")
            .setChooserTitle(R.string.edit_calendar_send_url)
            .startChooser()

}