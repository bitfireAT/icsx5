/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.model

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.platform.app.InstrumentationRegistry
import at.bitfire.ical4android.Css3Color
import at.bitfire.icsdroid.HttpUtils.toAndroidUri
import at.bitfire.icsdroid.ui.ResourceInfo
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class ValidationRepositoryTest {

    companion object {

        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application

        lateinit var server: MockWebServer

        @BeforeClass
        @JvmStatic
        fun setUp() {
            server = MockWebServer()
            server.start()
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            server.shutdown()
        }

    }

    @get:Rule
    val instantTaskExecutor = InstantTaskExecutorRule()


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
        server.enqueue(MockResponse().setBody(iCal))

        val model = ValidationRepository(app)
        runBlocking {
            // Wait until the validation completed
            model.validate(server.url("/").toAndroidUri(), null, null).join()
        }

        return model.uiState.result!!
    }

}