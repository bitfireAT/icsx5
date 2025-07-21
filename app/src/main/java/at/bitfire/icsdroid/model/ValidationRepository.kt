/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.model

import android.content.Context
import android.net.Uri
import android.util.Log
import at.bitfire.ical4android.Css3Color
import at.bitfire.ical4android.Event
import at.bitfire.ical4android.ICalendar
import at.bitfire.icsdroid.CalendarFetcher
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.HttpUtils.toURI
import at.bitfire.icsdroid.HttpUtils.toUri
import at.bitfire.icsdroid.ui.ResourceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.fortuna.ical4j.model.property.Color
import okhttp3.MediaType
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ValidationRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val _isValidating = MutableStateFlow(false)
    val isVerifyingUrl: StateFlow<Boolean> = _isValidating

    private val _result = MutableStateFlow<ResourceInfo?>(null)
    val result: StateFlow<ResourceInfo?> = _result

    fun resetResult() {
        _result.value = null
    }

    fun validate(
        originalUri: Uri,
        username: String?,
        password: String?
    ) = CoroutineScope(Dispatchers.IO).launch {
        try {
            Log.i(Constants.TAG, "Validating Webcal feed $originalUri (authentication: $username)")

            _isValidating.value = true

            val info = ResourceInfo(originalUri)
            val downloader = object: CalendarFetcher(context, originalUri) {
                override suspend fun onSuccess(
                    data: InputStream,
                    contentType: MediaType?,
                    eTag: String?,
                    lastModified: Long?,
                    displayName: String?
                ) {
                    InputStreamReader(data, contentType?.charset() ?: Charsets.UTF_8).use { reader ->
                        val properties = mutableMapOf<String, String>()
                        val events = Event.eventsFromReader(reader, properties)

                        info.calendarName = properties[ICalendar.CALENDAR_NAME] ?: displayName
                        info.calendarColor =
                            // try COLOR first
                            properties[Color.PROPERTY_NAME]?.let { colorValue ->
                                Css3Color.colorFromString(colorValue)
                            } ?:
                                // try X-APPLE-CALENDAR-COLOR second
                                try {
                                    properties[ICalendar.CALENDAR_COLOR]?.let { colorValue ->
                                        Css3Color.colorFromString(colorValue)
                                    }
                                } catch (e: IllegalArgumentException) {
                                    Log.w(Constants.TAG, "Couldn't parse calendar COLOR", e)
                                    null
                                }
                        info.eventsFound = events.size
                    }

                    _result.value = info
                }

                override suspend fun onNewPermanentUrl(target: Uri) {
                    Log.i(Constants.TAG, "Got permanent redirect when validating, saving new URL: $target")
                    val location = uri.toURI().resolve(target.toURI())
                    info.uri = location.toUri()
                }

                override suspend fun onError(error: Exception) {
                    Log.e(Constants.TAG, "Couldn't validate calendar", error)
                    info.exception = error
                    _result.value = info
                }
            }

            downloader.username = username
            downloader.password = password

            // directly ask for confirmation of custom certificates
            downloader.inForeground = true

            downloader.fetch()
        } finally {
            _isValidating.value = false
        }
    }

}
