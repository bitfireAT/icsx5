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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import at.bitfire.ical4android.Event
import at.bitfire.ical4android.ICalendar
import at.bitfire.icsdroid.CalendarFetcher
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.HttpClient
import at.bitfire.icsdroid.R
import okhttp3.MediaType
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL

class AddCalendarValidationFragment: DialogFragment() {

    private val titleColorModel by activityViewModels<TitleColorFragment.TitleColorModel>()
    private val credentialsModel by activityViewModels<CredentialsFragment.CredentialsModel>()

    private val validationModel by viewModels<ValidationModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        validationModel.result.observe(this) { info ->
            requireDialog().dismiss()

            val exception = info.exception
            if (exception == null) {
                titleColorModel.url.value = info.uri.toString()
                if (titleColorModel.color.value == null)
                    titleColorModel.color.value = resources.getColor(R.color.lightblue)

                if (titleColorModel.title.value.isNullOrBlank())
                    titleColorModel.title.value = info.calendarName ?: info.uri.file

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
        }
        // TODO: branch here based on url scheme "content" vs others
        val uri = Uri.parse(titleColorModel.url.value ?: throw IllegalArgumentException("No URL given"))!!
        val authenticate = credentialsModel.requiresAuth.value ?: false
        validationModel.initialize(uri,
                if (authenticate) credentialsModel.username.value else null,
                if (authenticate) credentialsModel.password.value else null)
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
            application: Application
    ): AndroidViewModel(application) {

        val result = MutableLiveData<ResourceInfo>()
        private var initialized = false

        fun initialize(originalUrl: Uri, username: String?, password: String?) {
            synchronized(initialized) {
                if (initialized)
                    return
                initialized = true
            }

            Log.i(Constants.TAG, "Validating Webcal feed $originalUrl (authentication: $username)")

            val info = ResourceInfo(originalUrl)
            val downloader = object: CalendarFetcher(getApplication(), originalUrl) {
                override fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?) {
                    InputStreamReader(data, contentType?.charset() ?: Charsets.UTF_8).use { reader ->
                        val properties = mutableMapOf<String, String>()
                        val events = Event.eventsFromReader(reader, properties)

                        info.calendarName = properties[ICalendar.CALENDAR_NAME]
                        info.eventsFound = events.size
                    }

                    result.postValue(info)
                }

                override fun onNewPermanentUrl(target: URL) {
                    Log.i(Constants.TAG, "Got permanent redirect when validating, saving new URL: $target")
                    info.uri = target
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

            Thread(downloader).start()
        }

    }

}
