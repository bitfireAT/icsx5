/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.model

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.platform.app.InstrumentationRegistry
import at.bitfire.ical4android.Css3Color
import at.bitfire.icsdroid.AppHttpClient
import at.bitfire.icsdroid.MockServer
import at.bitfire.icsdroid.ui.ResourceInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ValidationUseCaseTest {

    companion object {

        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application

    }

    @get:Rule
    val instantTaskExecutor = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        MockServer.clear()
    }

    @Test
    fun testModelInitialize_CalendarProperties_None() {
        val info = validate(
            "BEGIN:VCALENDAR\n" +
                    "VERSION:2.0\n" +
                    "END:VCALENDAR\n"
        )
        assertNull(info.calendarName)
        assertNull(info.calendarColor)
    }

    @Test
    fun testModelInitialize_CalendarProperties_NameAndColor() {
        val info = validate(
            "BEGIN:VCALENDAR\n" +
            "VERSION:2.0\n" +
            "X-WR-CALNAME:Some Calendar\n" +
            "COLOR:lightblue\n" +
            "END:VCALENDAR\n"
        )
        assertEquals("Some Calendar", info.calendarName)
        assertEquals(Css3Color.lightblue.argb, info.calendarColor)
    }

    @Test
    fun testModelInitialize_CalendarProperties_NameAndLegacyColor() {
        val info = validate(
            "BEGIN:VCALENDAR\n" +
                    "VERSION:2.0\n" +
                    "X-WR-CALNAME:Some Calendar\n" +
                    "X-APPLE-CALENDAR-COLOR:#123456\n" +
                    "END:VCALENDAR\n"
        )
        assertEquals(0xFF123456.toInt(), info.calendarColor)
    }

    @Test
    fun testModelInitialize_CalendarProperties_ColorAndLegacyColor() {
        val info = validate(
            "BEGIN:VCALENDAR\n" +
                    "VERSION:2.0\n" +
                    "X-APPLE-CALENDAR-COLOR:#123456\n" +
                    "COLOR:lightblue\n" +
                    "END:VCALENDAR\n"
        )
        assertEquals(Css3Color.lightblue.argb, info.calendarColor)
    }

    private fun validate(iCal: String): ResourceInfo {
        MockServer.enqueue(content = iCal)

        val client = AppHttpClient(app, MockServer.engine)
        val model = ValidationUseCase(app, client)
        runBlocking {
            // Wait until the validation completed
            model.validate(MockServer.uri(), null, null).join()
        }

        return model.uiState.result!!
    }

}