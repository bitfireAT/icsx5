/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.db

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.ContentValues
import android.os.RemoteException
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.util.Log
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.AndroidCalendarFactory
import at.bitfire.ical4android.CalendarStorageException
import at.bitfire.ical4android.util.MiscUtils.UriHelper.asSyncAdapter
import at.bitfire.icsdroid.Constants
import net.fortuna.ical4j.model.*
import net.fortuna.ical4j.model.component.VAlarm
import net.fortuna.ical4j.model.property.Action
import net.fortuna.ical4j.model.property.Description
import net.fortuna.ical4j.model.property.Duration
import net.fortuna.ical4j.model.property.Repeat
import net.fortuna.ical4j.model.property.Trigger
import java.time.temporal.TemporalAmount

class LocalCalendar private constructor(
    account: Account,
    provider: ContentProviderClient,
    id: Long
) : AndroidCalendar<LocalEvent>(account, provider, LocalEvent.Factory, id) {

    companion object {

        const val DEFAULT_COLOR = 0xFF2F80C7.toInt()

        const val COLUMN_ETAG = Calendars.CAL_SYNC1
        const val COLUMN_LAST_MODIFIED = Calendars.CAL_SYNC4
        const val COLUMN_LAST_SYNC = Calendars.CAL_SYNC5
        const val COLUMN_ERROR_MESSAGE = Calendars.CAL_SYNC6
        const val COLUMN_ALLOWED_REMINDERS = Calendars.ALLOWED_REMINDERS

        /**
         * Stores if the calendar's embed alerts should be ignored.
         * @since 20221202
         */
        const val COLUMN_IGNORE_EMBED = Calendars.CAL_SYNC8

        /**
         * TODO: Change javadoc
         * Stores the reminder set for all the events of the calendar.
         * @since 20221202
         */
        const val COLUMN_DEFAULT_ALARM = Calendars.CAL_SYNC7

        fun findById(account: Account, provider: ContentProviderClient, id: Long) =
            findByID(account, provider, Factory, id)

        fun findAll(account: Account, provider: ContentProviderClient) =
            find(account, provider, Factory, null, null)

    }

    var url: String? = null             // URL of iCalendar file
    var eTag: String? = null            // iCalendar ETag at last successful sync

    var lastModified = 0L               // iCalendar Last-Modified at last successful sync (or 0 for none)
    var lastSync = 0L                   // time of last sync (0 if none)
    var errorMessage: String? = null    // error message (HTTP status or exception name) of last sync (or null)

    var ignoreEmbedAlerts: Boolean? = null

    var defaultAlarmMinutes: Long? = null


    override fun populate(info: ContentValues) {
        super.populate(info)
        url = info.getAsString(Calendars.NAME)

        eTag = info.getAsString(COLUMN_ETAG)
        info.getAsLong(COLUMN_LAST_MODIFIED)?.let { lastModified = it }

        info.getAsLong(COLUMN_LAST_SYNC)?.let { lastSync = it }
        errorMessage = info.getAsString(COLUMN_ERROR_MESSAGE)

        info.getAsLong(COLUMN_DEFAULT_ALARM)
            ?.let { defaultAlarmMinutes = it }

        info.getAsBoolean(COLUMN_IGNORE_EMBED)
            ?.let { ignoreEmbedAlerts = it }
    }

    private fun updateAlarms() {
        queryEvents(null, null)
            .also { if (ignoreEmbedAlerts == true) Log.d(Constants.TAG, "Removing all alarms for ${it.size} events.") }
            .filter { it.event != null }
            .forEach { ev ->
                val event = ev.event!!
                if (ignoreEmbedAlerts == true) {
                    // Remove all alerts
                    event.unknownProperties.add(
                        PropertyBuilder()
                            .name("--ALARMS-BAK")
                            .value(
                                event.alarms.joinToString(";;") { it.toString() }
                            )
                            .build()
                    )
                    Log.d(Constants.TAG, "Removing all alarms from ${event.uid}: $event")
                    event.alarms.clear()
                    ev.update(event)
                } else {
                    // Add all the alarms back again
                    Log.d(Constants.TAG, "Adding all disabled alarms")
                    val props = event.unknownProperties
                        .find { it.name == "--ALARMS-BAK" }
                        ?.value
                        ?.split(Regex(";{2}"))
                        ?.map { alarmString ->
                            val properties = PropertyList<Property>()
                            properties.addAll(
                                alarmString.split('\n')
                                    .map { it.split(':') }
                                    .mapNotNull {
                                        val value = it.subList(1, it.size).joinToString(":")
                                        when (it[0]) {
                                            // TODO: Fix trigger parsing
                                            /*Property.TRIGGER -> {
                                                Log.d(Constants.TAG, "Trigger: \"$value\"")
                                                val adapter = TemporalAmountAdapter.parse(value)
                                                Trigger(adapter.duration)
                                            }*/
                                            Property.ACTION -> Action(value)
                                            Property.DESCRIPTION -> Description(value)
                                            Property.REPEAT -> Repeat(value.toInt())
                                            Property.DURATION -> Duration(TemporalAmountAdapter.parse(value).duration)
                                            else -> null
                                        }
                                    }
                            )
                            VAlarm.Factory().createComponent(properties)
                        }
                    Log.d(Constants.TAG, "Props: $props")
                }
            }
    }

    fun updateStatusSuccess(eTag: String?, lastModified: Long) {
        this.eTag = eTag
        this.lastModified = lastModified
        lastSync = System.currentTimeMillis()

        val values = ContentValues(7)
        values.put(COLUMN_ETAG, eTag)
        values.put(COLUMN_LAST_MODIFIED, lastModified)
        values.put(COLUMN_LAST_SYNC, lastSync)
        values.putNull(COLUMN_ERROR_MESSAGE)
        values.put(COLUMN_DEFAULT_ALARM, defaultAlarmMinutes)
        values.put(COLUMN_IGNORE_EMBED, ignoreEmbedAlerts)
        update(values)

        updateAlarms()
    }

    fun updateStatusNotModified() {
        lastSync = System.currentTimeMillis()

        val values = ContentValues(1)
        values.put(COLUMN_LAST_SYNC, lastSync)
        update(values)

        updateAlarms()
    }

    fun updateStatusError(message: String) {
        eTag = null
        lastModified = 0
        lastSync = System.currentTimeMillis()
        errorMessage = message

        val values = ContentValues(7)
        values.putNull(COLUMN_ETAG)
        values.putNull(COLUMN_LAST_MODIFIED)
        values.put(COLUMN_LAST_SYNC, lastSync)
        values.put(COLUMN_ERROR_MESSAGE, message)
        values.put(COLUMN_DEFAULT_ALARM, defaultAlarmMinutes)
        values.put(COLUMN_IGNORE_EMBED, ignoreEmbedAlerts)
        update(values)
    }

    fun updateUrl(url: String) {
        this.url = url

        val values = ContentValues(1)
        values.put(Calendars.NAME, url)
        update(values)
    }

    fun queryByUID(uid: String) =
        queryEvents("${Events._SYNC_ID}=?", arrayOf(uid))

    fun retainByUID(uids: MutableSet<String>): Int {
        var deleted = 0
        try {
            provider.query(
                Events.CONTENT_URI.asSyncAdapter(account),
                arrayOf(Events._ID, Events._SYNC_ID, Events.ORIGINAL_SYNC_ID),
                "${Events.CALENDAR_ID}=? AND ${Events.ORIGINAL_SYNC_ID} IS NULL", arrayOf(id.toString()), null
            )?.use { row ->
                while (row.moveToNext()) {
                    val eventId = row.getLong(0)
                    val syncId = row.getString(1)
                    if (!uids.contains(syncId)) {
                        provider.delete(ContentUris.withAppendedId(Events.CONTENT_URI, eventId).asSyncAdapter(account), null, null)
                        deleted++

                        uids -= syncId
                    }
                }
            }
            return deleted
        } catch (e: RemoteException) {
            throw CalendarStorageException("Couldn't delete local events")
        }
    }


    object Factory : AndroidCalendarFactory<LocalCalendar> {

        override fun newInstance(account: Account, provider: ContentProviderClient, id: Long) =
            LocalCalendar(account, provider, id)

    }

}
