/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.calendar

import at.bitfire.ical4android.Event
import java.util.Locale

internal object MicrosoftBusyStatus {

    const val PROPERTY_NAME = "X-MICROSOFT-CDO-BUSYSTATUS"

    enum class Value {
        FREE,
        TENTATIVE,
        BUSY,
        OOF,
        WORKINGELSEWHERE
    }

    fun prepareForAndroid(event: Event) {
        statusOf(event)?.let { status ->
            event.opaque = status != Value.FREE
        }
        event.exceptions.forEach(::prepareForAndroid)
    }

    fun statusOf(event: Event): Value? {
        val value = event.unknownProperties
            .firstOrNull { it.name.equals(PROPERTY_NAME, ignoreCase = true) }
            ?.value
            ?.trim()
            ?.uppercase(Locale.ROOT)
            ?: return null

        return Value.entries.firstOrNull { it.name == value }
    }

}
