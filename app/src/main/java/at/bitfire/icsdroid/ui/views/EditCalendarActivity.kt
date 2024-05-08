/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui.views

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.model.CredentialsModel
import at.bitfire.icsdroid.model.EditCalendarModel
import at.bitfire.icsdroid.model.EditSubscriptionModel
import at.bitfire.icsdroid.model.SubscriptionSettingsModel
import at.bitfire.icsdroid.ui.screen.EditCalendarScreen
import at.bitfire.icsdroid.ui.theme.setContentThemed
import kotlinx.coroutines.launch

class EditCalendarActivity: AppCompatActivity() {

    companion object {
        const val EXTRA_SUBSCRIPTION_ID = "subscriptionId"
    }

    private val subscriptionSettingsModel by viewModels<SubscriptionSettingsModel>()
    private val credentialsModel by viewModels<CredentialsModel>()

    private val editSubscriptionModel by viewModels<EditSubscriptionModel> {
        object: ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val subscriptionId = intent.getLongExtra(EXTRA_SUBSCRIPTION_ID, -1)
                return EditSubscriptionModel(application, subscriptionId) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentThemed {
            val successMessage = editSubscriptionModel.uiState.successMessage
            // show success message
            successMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                finish()
            }

            EditCalendarScreen(
                EditCalendarModel(
                    editSubscriptionModel,
                    subscriptionSettingsModel,
                    credentialsModel
                ),
                { onShare() },
                { finish() }
            )
        }
    }


    /* user actions */

    private fun onShare() {
        lifecycleScope.launch {
            editSubscriptionModel.subscriptionWithCredential.value?.let { (subscription, _) ->
                ShareCompat.IntentBuilder(this@EditCalendarActivity)
                    .setSubject(subscription.displayName)
                    .setText(subscription.url.toString())
                    .setType("text/plain")
                    .setChooserTitle(R.string.edit_calendar_send_url)
                    .startChooser()
            }
        }
    }

}