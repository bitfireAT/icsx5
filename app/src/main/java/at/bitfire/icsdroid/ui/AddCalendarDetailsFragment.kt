/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddCalendarDetailsFragment: Fragment() {

    private val titleColorModel by activityViewModels<TitleColorFragment.TitleColorModel>()
    private val credentialsModel by activityViewModels<CredentialsFragment.CredentialsModel>()
    private val creationModel by activityViewModels<CreateSubscriptionViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val invalidateOptionsMenu = Observer<Any> {
            requireActivity().invalidateOptionsMenu()
        }
        titleColorModel.title.observe(this, invalidateOptionsMenu)
        titleColorModel.color.observe(this, invalidateOptionsMenu)
        titleColorModel.ignoreAlerts.observe(this, invalidateOptionsMenu)
        titleColorModel.defaultAlarmMinutes.observe(this, invalidateOptionsMenu)

        // Set the default value to null so that the visibility of the summary is updated
        titleColorModel.defaultAlarmMinutes.postValue(null)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
        val v = inflater.inflate(R.layout.add_calendar_details, container, false)
        setHasOptionsMenu(true)

        return v
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_create_calendar, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val itemGo = menu.findItem(R.id.create_calendar)
        itemGo.isEnabled = !titleColorModel.title.value.isNullOrBlank()
    }

    override fun onOptionsItemSelected(item: MenuItem) =
            if (item.itemId == R.id.create_calendar) {
                creationModel.createSubscription(titleColorModel, credentialsModel)
                    .invokeOnCompletion {
                        // Finish the activity to go back to the subscriptions list
                        activity?.finish()
                    }
                true
            } else
                false

    class CreateSubscriptionViewModel(application: Application) : AndroidViewModel(application) {
        private val database = AppDatabase.getInstance(getApplication())
        private val subscriptionsDao = database.subscriptionsDao()
        private val credentialsDao = database.credentialsDao()

        /**
         * Creates a new subscription taking the data from the given models.
         */
        fun createSubscription(
            titleColorModel: TitleColorFragment.TitleColorModel,
            credentialsModel: CredentialsFragment.CredentialsModel,
        ) = viewModelScope.launch {
            val application = getApplication<Application>()

            try {
                val subscription = Subscription(
                    displayName = titleColorModel.title.value!!,
                    url = Uri.parse(titleColorModel.url.value),
                    color = titleColorModel.color.value,
                    ignoreEmbeddedAlerts = titleColorModel.ignoreAlerts.value ?: false,
                    defaultAlarmMinutes = titleColorModel.defaultAlarmMinutes.value
                )

                /** A list of all the ids of the inserted rows, should only contain one value */
                val ids = withContext(Dispatchers.IO) { subscriptionsDao.add(subscription) }

                /** The id of the newly inserted subscription */
                val id = ids.first()

                // Create the credential in the IO thread
                if (credentialsModel.requiresAuth.value == true) withContext(Dispatchers.IO) {
                    // If the subscription requires credentials, create them
                    val credential = Credential(
                        subscriptionId = id,
                        username = credentialsModel.username.value!!,
                        password = credentialsModel.password.value!!
                    )
                    credentialsDao.create(credential)
                }

                // Show a toast informing that the calendar has been created
                Toast.makeText(
                    application,
                    application.getString(R.string.add_calendar_created),
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Log.e(Constants.TAG, "Couldn't create calendar", e)
                Toast.makeText(application, e.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

}
