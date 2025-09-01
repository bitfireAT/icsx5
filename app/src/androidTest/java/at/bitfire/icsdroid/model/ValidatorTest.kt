/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.model

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.platform.app.InstrumentationRegistry
import at.bitfire.ical4android.Css3Color
import at.bitfire.icsdroid.AppHttpClient
import at.bitfire.icsdroid.MockServer
import at.bitfire.icsdroid.ui.ResourceInfo
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class ValidatorTest {

    @get:Rule
    val instantTaskExecutor = InstantTaskExecutorRule()

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var appHttpClientFactory: AppHttpClient.Factory

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        hiltRule.inject()
        MockServer.clear()

        appHttpClientFactory = object : AppHttpClient.Factory {
            override fun create(
                customUserAgent: String?,
                engine: HttpClientEngine
            ): AppHttpClient = MockServer.httpClient(context)
        }
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

        val validator = Validator(context, appHttpClientFactory)
        return runBlocking {
            // Wait until the validation completed
            validator.validate(
                MockServer.uri(),
                null,
                null,
                null
            )
        }
    }

}