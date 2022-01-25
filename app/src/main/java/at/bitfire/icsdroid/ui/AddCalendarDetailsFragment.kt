/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.ContentValues
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.MiscUtils.ContentProviderClientHelper.closeCompat
import at.bitfire.icsdroid.AppAccount
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.CalendarCredentials
import at.bitfire.icsdroid.db.LocalCalendar

class AddCalendarDetailsFragment: Fragment() {

    private val titleColorModel by activityViewModels<TitleColorFragment.TitleColorModel>()
    private val credentialsModel by activityViewModels<CredentialsFragment.CredentialsModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val invalidateOptionsMenu = Observer<Any> {
            requireActivity().invalidateOptionsMenu()
        }
        titleColorModel.title.observe(this, invalidateOptionsMenu)
        titleColorModel.color.observe(this, invalidateOptionsMenu)
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
                if (createCalendar())
                    requireActivity().finish()
                true
            } else
                false


    private fun createCalendar(): Boolean {
        val account = AppAccount.get(requireActivity())

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

        val client: ContentProviderClient? = requireActivity().contentResolver.acquireContentProviderClient(CalendarContract.AUTHORITY)
        return try {
            client?.let {
                val uri = AndroidCalendar.create(account, it, calInfo)
                val calendar = LocalCalendar.findById(account, client, ContentUris.parseId(uri))
                CalendarCredentials(requireActivity()).put(calendar, credentialsModel.username.value, credentialsModel.password.value)
            }
            Toast.makeText(activity, getString(R.string.add_calendar_created), Toast.LENGTH_LONG).show()
            requireActivity().invalidateOptionsMenu()
            true
        } catch(e: Exception) {
            Log.e(Constants.TAG, "Couldn't create calendar", e)
            Toast.makeText(context, e.localizedMessage, Toast.LENGTH_LONG).show()
            false
        } finally {
            client?.closeCompat()
        }
    }

}
