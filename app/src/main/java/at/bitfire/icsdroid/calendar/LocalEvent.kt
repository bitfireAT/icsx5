/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.calendar

import android.content.ContentValues
import android.content.ContentUris
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.ExtendedProperties
import at.bitfire.ical4android.AndroidEvent
import at.bitfire.ical4android.Event
import at.bitfire.ical4android.UnknownProperty
import at.bitfire.ical4android.util.MiscUtils.asSyncAdapter
import java.util.logging.Logger

class LocalEvent(
    val androidEvent: AndroidEvent
) {

    val lastModified
        get() = androidEvent.lastModified

    fun add() = persist(androidEvent.event!!) {
        androidEvent.add()
    }

    fun update(event: Event) = persist(event) {
        androidEvent.update(event)
    }

    private inline fun <T> persist(event: Event, operation: () -> T): T {
        if (androidEvent.syncId == null)
            androidEvent.syncId = event.uid
        MicrosoftBusyStatus.prepareForAndroid(event)
        val result = operation()
        persistAndroidAvailability(event)
        persistExceptionUnknownProperties(event)
        return result
    }

    private fun persistAndroidAvailability(event: Event) {
        if (MicrosoftBusyStatus.statusOf(event) == MicrosoftBusyStatus.Value.TENTATIVE) {
            androidEvent.update(ContentValues(1).apply {
                put(Events.AVAILABILITY, Events.AVAILABILITY_TENTATIVE)
            })
        }

        persistedExceptions(event).forEach { (exception, id) ->
            if (MicrosoftBusyStatus.statusOf(exception) == MicrosoftBusyStatus.Value.TENTATIVE) {
                androidEvent.calendar.provider.update(
                    ContentUris.withAppendedId(Events.CONTENT_URI, id)
                        .asSyncAdapter(androidEvent.calendar.account),
                    ContentValues(1).apply {
                        put(Events.AVAILABILITY, Events.AVAILABILITY_TENTATIVE)
                    },
                    null,
                    null
                )
            }
        }
    }

    private fun persistExceptionUnknownProperties(event: Event) {
        persistedExceptions(event).forEach { (exception, id) ->
            for (property in exception.unknownProperties) {
                val value = property.value
                if (value == null) {
                    logger.warning("Ignoring unknown exception property with null value")
                    continue
                }
                if (value.length > UnknownProperty.MAX_UNKNOWN_PROPERTY_SIZE) {
                    logger.warning(
                        "Ignoring unknown exception property with ${value.length} octets (too long)"
                    )
                    continue
                }

                androidEvent.calendar.provider.insert(
                    ExtendedProperties.CONTENT_URI.asSyncAdapter(androidEvent.calendar.account),
                    ContentValues(3).apply {
                        put(ExtendedProperties.EVENT_ID, id)
                        put(ExtendedProperties.NAME, UnknownProperty.CONTENT_ITEM_TYPE)
                        put(ExtendedProperties.VALUE, UnknownProperty.toJsonString(property))
                    }
                )
            }
        }
    }

    private fun persistedExceptions(event: Event): List<Pair<Event, Long>> {
        val eventId = requireNotNull(androidEvent.id)
        val exceptions = event.exceptions.filter { it.recurrenceId != null }
        if (exceptions.isEmpty())
            return emptyList()

        val ids = mutableListOf<Long>()
        androidEvent.calendar.provider.query(
            Events.CONTENT_URI.asSyncAdapter(androidEvent.calendar.account),
            arrayOf(Events._ID),
            "${Events.ORIGINAL_ID}=?",
            arrayOf(eventId.toString()),
            "${Events._ID} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext())
                ids += cursor.getLong(0)
        }

        if (ids.size != exceptions.size) {
            logger.warning(
                "Expected ${exceptions.size} persisted exceptions for event $eventId, found ${ids.size}"
            )
        }
        return exceptions.zip(ids)
    }

    companion object {
        private val logger
            get() = Logger.getLogger(LocalEvent::class.java.name)
    }
}