/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.database.SQLException
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.annotation.WorkerThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import at.bitfire.icsdroid.AppAccount
import at.bitfire.icsdroid.Constants.TAG
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.CalendarCredentials
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.utils.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.MalformedURLException

@Deprecated("Use Jetpack Compose")
class AddCalendarDetailsFragment : Fragment() {

    private val titleColorModel by activityViewModels<TitleColorFragment.TitleColorModel>()
    private val credentialsModel by activityViewModels<CredentialsFragment.CredentialsModel>()

    private val calendarCreated = MutableLiveData<Boolean>(false)

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

        // Finish activity when calendar is created
        calendarCreated.observe(this) { if (it) requireActivity().finish() }
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
            CoroutineScope(Dispatchers.IO).launch { createCalendar() }
            true
        } else
            false


    @WorkerThread
    private suspend fun createCalendar() {
        val account = AppAccount.get(requireActivity())

        try {
            val subscription = Subscription(
                id = 0L,
                eTag = null,
                lastModified = 0L,
                lastSync = 0L,
                url = Uri.parse(titleColorModel.url.value!!),
                displayName = titleColorModel.title.value!!,
                color = titleColorModel.color.value,
                ignoreEmbeddedAlerts = titleColorModel.ignoreAlerts.value ?: false,
                defaultAlarmMinutes = titleColorModel.defaultAlarmMinutes.value,
                accountName = account.name,
                accountType = account.type,
            )

            if (credentialsModel.requiresAuth.value == true)
                CalendarCredentials(requireActivity()).put(subscription, credentialsModel.username.value, credentialsModel.password.value)

            Log.v(TAG, "Adding subscription to database...")
            AppDatabase.getInstance(requireContext())
                .subscriptionsDao()
                .add(subscription)

            Log.v(TAG, "Adding subscription to system...")
            subscription.createAndroidCalendar(requireContext())

            withContext(Dispatchers.Main) {
                toast(R.string.add_calendar_created)
                requireActivity().invalidateOptionsMenu()
            }

            calendarCreated.postValue(true)
        } catch (e: SQLException) {
            Log.e(TAG, "Couldn't create calendar", e)
            e.localizedMessage?.let { withContext(Dispatchers.Main) { toast(it).show() } }
            calendarCreated.postValue(false)
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Couldn't create calendar", e)
            e.localizedMessage?.let { withContext(Dispatchers.Main) { toast(it).show() } }
            calendarCreated.postValue(false)
        }
    }

}
