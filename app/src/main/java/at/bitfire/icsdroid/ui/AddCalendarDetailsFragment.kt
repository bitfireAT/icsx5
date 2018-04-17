/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui

import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.ContentValues
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.support.v4.app.Fragment
import android.util.Log
import android.view.*
import android.widget.Toast
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.icsdroid.AppAccount
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.CalendarCredentials
import at.bitfire.icsdroid.db.LocalCalendar
import at.bitfire.icsdroid.ui.AddCalendarActivity.Companion.EXTRA_COLOR
import at.bitfire.icsdroid.ui.AddCalendarActivity.Companion.EXTRA_TITLE

class AddCalendarDetailsFragment: Fragment(), TitleColorFragment.OnChangeListener {

    companion object {

        const val ARG_INFO = "info"

        private const val STATE_TITLE = "title"
        private const val STATE_COLOR = "color"

        fun newInstance(info: ResourceInfo): AddCalendarDetailsFragment {
            val frag = AddCalendarDetailsFragment()
            val args = Bundle(1)
            args.putSerializable(ARG_INFO, info)
            frag.arguments = args
            return frag
        }

    }

    private lateinit var info: ResourceInfo

    private var title: String? = null
    private var color: Int = LocalCalendar.DEFAULT_COLOR



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        info = arguments!!.getSerializable(ARG_INFO) as ResourceInfo
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
        val v = inflater.inflate(R.layout.add_calendar_details, container, false)
        setHasOptionsMenu(true)

        if (inState != null) {
            title = inState.getString(STATE_TITLE)
            color = inState.getInt(STATE_COLOR)
        } else {
            title = activity?.intent?.getStringExtra(EXTRA_TITLE) ?: info.calendarName
            color = activity?.intent?.getIntExtra(EXTRA_COLOR, LocalCalendar.DEFAULT_COLOR) ?: LocalCalendar.DEFAULT_COLOR
        }

        val fragTitleColor = TitleColorFragment()
        val args = Bundle(3)
        args.putString(TitleColorFragment.ARG_URL, info.url.toString())
        args.putString(TitleColorFragment.ARG_TITLE, title)
        args.putInt(TitleColorFragment.ARG_COLOR, color)
        fragTitleColor.arguments = args
        fragTitleColor.setOnChangeListener(this)
        childFragmentManager.beginTransaction()
                .replace(R.id.title_color, fragTitleColor)
                .commit()
        return v
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_TITLE, title)
        outState.putInt(STATE_COLOR, color)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_create_calendar, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val itemGo = menu.findItem(R.id.create_calendar)
        itemGo.isEnabled = !title.isNullOrBlank()
    }

    override fun onOptionsItemSelected(item: MenuItem) =
            if (item.itemId == R.id.create_calendar) {
                if (createCalendar())
                    activity!!.finish()
                true
            } else
                false


    override fun onChangeTitleColor(title: String?, color: Int) {
        this.title = title
        this.color = color

        activity!!.invalidateOptionsMenu()
    }


    private fun createCalendar(): Boolean {
        AppAccount.makeAvailable(activity!!)

        val calInfo = ContentValues(9)
        calInfo.put(Calendars.ACCOUNT_NAME, AppAccount.account.name)
        calInfo.put(Calendars.ACCOUNT_TYPE, AppAccount.account.type)
        calInfo.put(Calendars.NAME, info.url.toString())
        calInfo.put(Calendars.CALENDAR_DISPLAY_NAME, title)
        calInfo.put(Calendars.CALENDAR_COLOR, color)
        calInfo.put(Calendars.OWNER_ACCOUNT, AppAccount.account.name)
        calInfo.put(Calendars.SYNC_EVENTS, 1)
        calInfo.put(Calendars.VISIBLE, 1)
        calInfo.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_READ)

        val client: ContentProviderClient? = activity!!.contentResolver.acquireContentProviderClient(CalendarContract.AUTHORITY)
        return try {
            client?.let {
                val uri = AndroidCalendar.create(AppAccount.account, it, calInfo)
                val calendar = LocalCalendar.findById(AppAccount.account, client, ContentUris.parseId(uri))
                CalendarCredentials.putCredentials(activity!!, calendar, info.username, info.password)
            }
            Toast.makeText(activity, getString(R.string.add_calendar_created), Toast.LENGTH_LONG).show()
            activity!!.invalidateOptionsMenu()
            true
        } catch(e: Exception) {
            Log.e(Constants.TAG, "Couldn't create calendar", e)
            Toast.makeText(context, e.localizedMessage, Toast.LENGTH_LONG).show()
            false
        } finally {
            client?.release()
        }
    }

}
