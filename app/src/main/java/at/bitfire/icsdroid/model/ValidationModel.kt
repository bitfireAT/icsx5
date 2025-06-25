/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.model

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.bitfire.ical4android.Css3Color
import at.bitfire.ical4android.Event
import at.bitfire.ical4android.ICalendar
import at.bitfire.icsdroid.CalendarFetcher
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.HttpUtils.toURI
import at.bitfire.icsdroid.HttpUtils.toUri
import at.bitfire.icsdroid.ui.ResourceInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.fortuna.ical4j.model.property.Color
import okhttp3.MediaType
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject

@HiltViewModel
class ValidationModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
): ViewModel() {

    data class UiState(
        val isVerifyingUrl: Boolean = false,
        val result: ResourceInfo? = null
    )

    var uiState by mutableStateOf(UiState())
        private set

    fun resetResult() {
        uiState = uiState.copy(result = null)
    }

    fun validate(
        originalUri: Uri,
        username: String?,
        password: String?
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            Log.i(Constants.TAG, "Validating Webcal feed $originalUri (authentication: $username)")

            uiState = uiState.copy(isVerifyingUrl = true)

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

                    uiState = uiState.copy(result = info)
                }

                override suspend fun onNewPermanentUrl(target: Uri) {
                    Log.i(Constants.TAG, "Got permanent redirect when validating, saving new URL: $target")
                    val location = uri.toURI().resolve(target.toURI())
                    info.uri = location.toUri()
                }

                override suspend fun onError(error: Exception) {
                    Log.e(Constants.TAG, "Couldn't validate calendar", error)
                    info.exception = error
                    uiState = uiState.copy(result = info)
                }
            }

            downloader.username = username
            downloader.password = password

            // directly ask for confirmation of custom certificates
            downloader.inForeground = true

            downloader.fetch()
        } finally {
            uiState = uiState.copy(isVerifyingUrl = false)
        }
    }

}
