package at.bitfire.icsdroid.model

import android.content.Context
import android.net.Uri
import android.util.Log
import at.bitfire.ical4android.Css3Color
import at.bitfire.ical4android.Event
import at.bitfire.ical4android.ICalendar
import at.bitfire.icsdroid.AppHttpClient
import at.bitfire.icsdroid.CalendarFetcher
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.HttpUtils.toURI
import at.bitfire.icsdroid.HttpUtils.toUri
import at.bitfire.icsdroid.ui.ResourceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.ContentType
import io.ktor.http.charset
import net.fortuna.ical4j.model.property.Color
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ValidationUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appHttpClient: AppHttpClient,
) {
    suspend fun validate(
        originalUri: Uri,
        username: String?,
        password: String?
    ): ResourceInfo {
        Log.i(Constants.TAG, "Validating Webcal feed $originalUri (authentication: $username)")

        val info = ResourceInfo(originalUri)
        val downloader = object: CalendarFetcher(context, originalUri, appHttpClient) {
            override suspend fun onSuccess(
                data: InputStream,
                contentType: ContentType?,
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
                        } ?: run {
                            // try X-APPLE-CALENDAR-COLOR second
                            try {
                                properties[ICalendar.CALENDAR_COLOR]?.let { colorValue ->
                                    Css3Color.colorFromString(colorValue)
                                }
                            } catch (e: IllegalArgumentException) {
                                Log.w(Constants.TAG, "Couldn't parse calendar COLOR", e)
                                null
                            }
                        }
                    info.eventsFound = events.size
                }
            }

            override suspend fun onNewPermanentUrl(target: Uri) {
                Log.i(Constants.TAG, "Got permanent redirect when validating, saving new URL: $target")
                val location = uri.toURI().resolve(target.toURI())
                info.uri = location.toUri()
            }

            override suspend fun onError(error: Exception) {
                Log.e(Constants.TAG, "Couldn't validate calendar", error)
                info.exception = error
            }
        }

        downloader.username = username
        downloader.password = password

        // directly ask for confirmation of custom certificates
        downloader.inForeground = true

        // downloader.fetch blocks the current thread until the request is completed
        downloader.fetch()

        // at this point, info's contents has already been updated correctly
        return info
    }
}