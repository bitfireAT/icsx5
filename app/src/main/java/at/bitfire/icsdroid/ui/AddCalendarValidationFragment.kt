/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.app.Application
import android.app.Dialog
import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import at.bitfire.ical4android.Css3Color
import at.bitfire.ical4android.Event
import at.bitfire.ical4android.ICalendar
import at.bitfire.icsdroid.CalendarFetcher
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.HttpClient
import at.bitfire.icsdroid.HttpUtils.toURI
import at.bitfire.icsdroid.HttpUtils.toUri
import at.bitfire.icsdroid.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.fortuna.ical4j.model.property.Color
import okhttp3.MediaType
import java.io.InputStream
import java.io.InputStreamReader

@Deprecated("Use Jetpack Compose")
class AddCalendarValidationFragment: DialogFragment() {

    private val titleColorModel by activityViewModels<TitleColorFragment.TitleColorModel>()
    private val credentialsModel by activityViewModels<CredentialsFragment.CredentialsModel>()

    private val validationModel by viewModels<ValidationModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val uri = Uri.parse(titleColorModel.url.value ?: throw IllegalArgumentException("No URL given"))!!
                val authenticate = credentialsModel.requiresAuth.value ?: false
                return ValidationModel(
                    requireActivity().application,
                    uri,
                    if (authenticate) credentialsModel.username.value else null,
                    if (authenticate) credentialsModel.password.value else null
                ) as T
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        validationModel.result.observe(this, Observer { info ->
            requireDialog().dismiss()

            val exception = info.exception
            if (exception == null) {
                titleColorModel.url.value = info.uri.toString()

                if (titleColorModel.color.value == null)
                    titleColorModel.color.value = info.calendarColor ?: ContextCompat.getColor(requireContext(), R.color.lightblue)

                if (titleColorModel.title.value.isNullOrBlank())
                    titleColorModel.title.value = info.calendarName ?: info.uri.toString()

                parentFragmentManager
                    .beginTransaction()
                    .replace(android.R.id.content, AddCalendarDetailsFragment())
                    .addToBackStack(null)
                    .commitAllowingStateLoss()
            } else {
                val errorMessage =
                    exception.localizedMessage ?: exception.message ?: exception.toString()
                AlertFragment.create(errorMessage, exception).show(parentFragmentManager, null)
            }
        })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val progress = ProgressDialog(activity)
        progress.setMessage(getString(R.string.add_calendar_validating))
        return progress
    }

    override fun onPause() {
        super.onPause()
        HttpClient.setForeground(false)
    }

    override fun onResume() {
        super.onResume()
        HttpClient.setForeground(true)
    }


    /* activityModel and data source */

    class ValidationModel(
            val context: Application,
            val originalUri: Uri,
            val username: String?,
            val password: String?
    ): ViewModel() {

        val result = MutableLiveData<ResourceInfo>()

        init {
            viewModelScope.launch(Dispatchers.Default) {
                validate()
            }
        }

        private suspend fun validate() {
            Log.i(Constants.TAG, "Validating Webcal feed $originalUri (authentication: $username)")

            val info = ResourceInfo(originalUri)
            val downloader = object: CalendarFetcher(context, originalUri) {
                override suspend fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?, displayName: String?) {
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

                override suspend fun onNewPermanentUrl(target: Uri) {
                    Log.i(Constants.TAG, "Got permanent redirect when validating, saving new URL: $target")
                    val location = uri.toURI().resolve(target.toURI())
                    info.uri = location.toUri()
                }

                override suspend fun onError(error: Exception) {
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