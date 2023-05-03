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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.SyncWorker
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddCalendarDetailsFragment: Fragment() {

    private val subscriptionSettingsModel by activityViewModels<SubscriptionSettingsFragment.SubscriptionSettingsModel>()
    private val credentialsModel by activityViewModels<CredentialsFragment.CredentialsModel>()
    private val model by activityViewModels<SubscriptionModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val invalidateOptionsMenu = Observer<Any> {
            requireActivity().invalidateOptionsMenu()
        }
        subscriptionSettingsModel.title.observe(this, invalidateOptionsMenu)
        subscriptionSettingsModel.color.observe(this, invalidateOptionsMenu)
        subscriptionSettingsModel.ignoreAlerts.observe(this, invalidateOptionsMenu)
        subscriptionSettingsModel.defaultAlarmMinutes.observe(this, invalidateOptionsMenu)
        subscriptionSettingsModel.defaultAllDayAlarmMinutes.observe(this, invalidateOptionsMenu)

        // Set the default value to null so that the visibility of the summary is updated
        subscriptionSettingsModel.defaultAlarmMinutes.value = null
        subscriptionSettingsModel.defaultAllDayAlarmMinutes.value = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
        val v = inflater.inflate(R.layout.add_calendar_details, container, false)
        setHasOptionsMenu(true)

        // Handle status changes
        model.success.observe(viewLifecycleOwner) { success ->
            if (success) {
                // success, show notification and close activity
                Toast.makeText(requireActivity(), requireActivity().getString(R.string.add_calendar_created),Toast.LENGTH_LONG).show()

                requireActivity().finish()
            }
        }
        model.errorMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show()
        }

        return v
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_create_calendar, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val itemGo = menu.findItem(R.id.create_calendar)
        itemGo.isEnabled = !subscriptionSettingsModel.title.value.isNullOrBlank()
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == R.id.create_calendar) {
            model.create(subscriptionSettingsModel, credentialsModel)
            true
        } else
            false


    class SubscriptionModel(application: Application) : AndroidViewModel(application) {

        private val database = AppDatabase.getInstance(getApplication())
        private val subscriptionsDao = database.subscriptionsDao()
        private val credentialsDao = database.credentialsDao()

        val success = MutableLiveData(false)
        val errorMessage = MutableLiveData<String>()

        /**
         * Creates a new subscription taking the data from the given models.
         */
        fun create(
            subscriptionSettingsModel: SubscriptionSettingsFragment.SubscriptionSettingsModel,
            credentialsModel: CredentialsFragment.CredentialsModel,
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val subscription = Subscription(
                        displayName = subscriptionSettingsModel.title.value!!,
                        url = Uri.parse(subscriptionSettingsModel.url.value),
                        color = subscriptionSettingsModel.color.value,
                        ignoreEmbeddedAlerts = subscriptionSettingsModel.ignoreAlerts.value ?: false,
                        defaultAlarmMinutes = subscriptionSettingsModel.defaultAlarmMinutes.value,
                        defaultAllDayAlarmMinutes = subscriptionSettingsModel.defaultAllDayAlarmMinutes.value,
                    )

                    /** A list of all the ids of the inserted rows */
                    val id = subscriptionsDao.add(subscription)

                    // Create the credential in the IO thread
                    if (credentialsModel.requiresAuth.value == true) {
                        // If the subscription requires credentials, create them
                        val username = credentialsModel.username.value
                        val password = credentialsModel.password.value
                        if (username != null && password != null) {
                            val credential = Credential(
                                subscriptionId = id,
                                username = username,
                                password = password
                            )
                            credentialsDao.create(credential)
                        }
                    }

                    // sync the subscription to reflect the changes in the calendar provider
                    SyncWorker.run(getApplication())

                    success.postValue(true)
                } catch (e: Exception) {
                    Log.e(Constants.TAG, "Couldn't create calendar", e)
                    errorMessage.postValue(e.localizedMessage)
                }
            }
        }
    }

}