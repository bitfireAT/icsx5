/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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

    private val titleColorModel by activityViewModels<SubscriptionSettingsFragment.TitleColorModel>()
    private val credentialsModel by activityViewModels<CredentialsFragment.CredentialsModel>()
    private val model by activityViewModels<SubscriptionModel>()

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

        // Handle status changes
        model.success.observe(viewLifecycleOwner) { success ->
            if (success) {
                // success, show notification and close activity
                Toast.makeText(
                    requireActivity(),
                    requireActivity().getString(R.string.add_calendar_created),
                    Toast.LENGTH_LONG
                ).show()

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
        itemGo.isEnabled = !titleColorModel.title.value.isNullOrBlank()
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == R.id.create_calendar) {
            model.create(titleColorModel, credentialsModel)
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
            titleColorModel: SubscriptionSettingsFragment.TitleColorModel,
            credentialsModel: CredentialsFragment.CredentialsModel,
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val subscription = Subscription(
                        displayName = titleColorModel.title.value!!,
                        url = Uri.parse(titleColorModel.url.value),
                        color = titleColorModel.color.value,
                        ignoreEmbeddedAlerts = titleColorModel.ignoreAlerts.value ?: false,
                        defaultAlarmMinutes = titleColorModel.defaultAlarmMinutes.value,
                        defaultAllDayAlarmMinutes = titleColorModel.defaultAllDayAlarmMinutes.value,
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