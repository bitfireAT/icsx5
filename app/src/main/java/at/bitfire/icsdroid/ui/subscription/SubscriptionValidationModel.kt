package at.bitfire.icsdroid.ui.subscription

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import at.bitfire.ical4android.Css3Color
import at.bitfire.ical4android.Event
import at.bitfire.ical4android.ICalendar
import at.bitfire.icsdroid.CalendarFetcher
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.HttpUtils.toURI
import at.bitfire.icsdroid.HttpUtils.toUri
import at.bitfire.icsdroid.ui.ResourceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.fortuna.ical4j.model.property.Color
import okhttp3.MediaType
import java.io.InputStream
import java.io.InputStreamReader

class SubscriptionValidationModel(
    application: Application
): AndroidViewModel(application) {

    val result = MutableLiveData<ResourceInfo>()

    fun validate(uri: Uri, username: String?, password: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.i(Constants.TAG, "Validating Webcal feed $uri (authentication: $username)")

            val info = ResourceInfo(uri)
            val downloader = object: CalendarFetcher(getApplication(), uri) {
                override fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?, displayName: String?) {
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

                    result.postValue(info)
                }

                override fun onNewPermanentUrl(target: Uri) {
                    Log.i(Constants.TAG, "Got permanent redirect when validating, saving new URL: $target")
                    val location = uri.toURI().resolve(target.toURI())
                    info.uri = location.toUri()
                }

                override fun onError(error: Exception) {
                    Log.e(Constants.TAG, "Couldn't validate calendar", error)
                    info.exception = error
                    result.postValue(info)
                }
            }

            downloader.username = username
            downloader.password = password

            // directly ask for confirmation of custom certificates
            downloader.inForeground = true

            downloader.fetch()
        }
    }

}
