/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.calendar

import at.bitfire.ical4android.Event
import net.fortuna.ical4j.model.property.ProdId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.StringReader

class MicrosoftBusyStatusTest {

    @Test
    fun recognizedValuesMapToAndroidSemantics() {
        val cases = mapOf(
            "FREE" to false,
            "tentative" to true,
            "BUSY" to true,
            "oof" to true,
            "WORKINGELSEWHERE" to true
        )

        cases.forEach { (value, expectedOpaque) ->
            val event = parseEvent(value)

            MicrosoftBusyStatus.prepareForAndroid(event)

            assertEquals(
                MicrosoftBusyStatus.Value.valueOf(value.uppercase()),
                MicrosoftBusyStatus.statusOf(event)
            )
            assertEquals(expectedOpaque, event.opaque)
        }
    }

    @Test
    fun propertyNameAndValueAreCaseInsensitive() {
        val event = parseEvent(" tentative ", propertyName = "x-microsoft-cdo-busystatus")

        MicrosoftBusyStatus.prepareForAndroid(event)

        assertEquals(MicrosoftBusyStatus.Value.TENTATIVE, MicrosoftBusyStatus.statusOf(event))
        assertTrue(event.opaque)
    }

    @Test
    fun unknownValueDoesNotOverrideTransparency() {
        val event = parseEvent("FOCUS", transparent = true)

        MicrosoftBusyStatus.prepareForAndroid(event)

        assertNull(MicrosoftBusyStatus.statusOf(event))
        assertFalse(event.opaque)
    }

    @Test
    fun rawPropertySurvivesExportRoundTrip() {
        val event = parseEvent("WorkingElsewhere")
        MicrosoftBusyStatus.prepareForAndroid(event)
        val output = ByteArrayOutputStream()

        event.write(output, ProdId("-//ICSx5 test//EN"))

        val roundTripped = Event.eventsFromReader(StringReader(output.toString(Charsets.UTF_8))).single()
        val property = roundTripped.unknownProperties.single {
            it.name.equals(MicrosoftBusyStatus.PROPERTY_NAME, ignoreCase = true)
        }
        assertEquals("WorkingElsewhere", property.value)
        assertEquals(MicrosoftBusyStatus.Value.WORKINGELSEWHERE, MicrosoftBusyStatus.statusOf(roundTripped))
    }

    private fun parseEvent(
        value: String,
        propertyName: String = MicrosoftBusyStatus.PROPERTY_NAME,
        transparent: Boolean = false
    ): Event {
        val lines = mutableListOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "PRODID:-//ICSx5 test//EN",
            "BEGIN:VEVENT",
            "UID:test@example.com",
            "DTSTART:20260807T120000Z",
            "DTEND:20260807T130000Z"
        )
        if (transparent)
            lines += "TRANSP:TRANSPARENT"
        lines += "$propertyName:$value"
        lines += "END:VEVENT"
        lines += "END:VCALENDAR"
        val ics = lines.joinToString("\r\n", postfix = "\r\n")
        return Event.eventsFromReader(StringReader(ics)).single()
    }

}
