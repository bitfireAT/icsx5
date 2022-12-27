/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.ContentValues
import android.database.SQLException
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.util.MiscUtils.ContentProviderClientHelper.closeCompat
import at.bitfire.icsdroid.*
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.CalendarCredentials
import at.bitfire.icsdroid.db.LocalCalendar
import at.bitfire.icsdroid.db.entity.Subscription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddCalendarDetailsFragment: Fragment() {

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
                doAsync { createCalendar() }
                true
            } else
                false


    @WorkerThread
    private suspend fun createCalendar() {
        val account = AppAccount.get(requireActivity())

        val subscription = Subscription(
            id = 0L,
            eTag = null,
            lastModified = 0L,
            lastSync = 0L,
            url = titleColorModel.url.value,
            displayName = titleColorModel.title.value,
            color = titleColorModel.color.value,
            ignoreEmbeddedAlerts = titleColorModel.ignoreAlerts.value ?: false,
            defaultAlarmMinutes = titleColorModel.defaultAlarmMinutes.value,
            accountName = account.name,
            accountType = account.type,
        )

        val calInfo = ContentValues(9)
        calInfo.put(Calendars.ACCOUNT_NAME, account.name)
        calInfo.put(Calendars.ACCOUNT_TYPE, account.type)
        calInfo.put(Calendars.NAME, titleColorModel.url.value)
        calInfo.put(Calendars.CALENDAR_DISPLAY_NAME, titleColorModel.title.value)
        calInfo.put(Calendars.CALENDAR_COLOR, titleColorModel.color.value)
        calInfo.put(Calendars.OWNER_ACCOUNT, account.name)
        calInfo.put(Calendars.SYNC_EVENTS, 1)
        calInfo.put(Calendars.VISIBLE, 1)
        calInfo.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_READ)
        calInfo.put(LocalCalendar.COLUMN_IGNORE_EMBEDDED, titleColorModel.ignoreAlerts.value)
        calInfo.put(LocalCalendar.COLUMN_DEFAULT_ALARM, titleColorModel.defaultAlarmMinutes.value)

        try {
            val database = AppDatabase.getInstance(requireContext())
            val dao = database.subscriptionsDao()
            dao.add(subscription)
            ui {
                Toast.makeText(activity, getString(R.string.add_calendar_created), Toast.LENGTH_LONG).show()
                requireActivity().invalidateOptionsMenu()
            }
            calendarCreated.postValue(true)
        } catch (e: SQLException) {
            Log.e(Constants.TAG, "Couldn't create calendar", e)
            ui { Toast.makeText(context, e.localizedMessage, Toast.LENGTH_LONG).show() }
            calendarCreated.postValue(false)
        }
    }

}
