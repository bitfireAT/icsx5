package at.bitfire.icsdroid.ui.model

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import at.bitfire.ical4android.Css3Color
import at.bitfire.ical4android.Event
import at.bitfire.ical4android.ICalendar
import at.bitfire.icsdroid.CalendarFetcher
import at.bitfire.icsdroid.Constants.TAG
import at.bitfire.icsdroid.HttpUtils
import at.bitfire.icsdroid.HttpUtils.toURI
import at.bitfire.icsdroid.HttpUtils.toUri
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.ui.ResourceInfo
import at.bitfire.icsdroid.utils.getString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URI
import java.net.URISyntaxException

class CreateSubscriptionModel(application: Application) : SubscriptionDetailsModel(application) {
    companion object {
        private val CALENDAR_MIME_TYPES = arrayOf("text/calendar")
    }

    val url = mutableStateOf("")
    val urlError = mutableStateOf<String?>(null)
    val insecureUrlWarning = mutableStateOf(false)

    val fileUri = mutableStateOf<Uri?>(null)
    val fileName = mutableStateOf<String?>(null)

    /**
     * Stores the type of subscription on the first page of the subscription creation wizard.
     */
    val selectionType = mutableStateOf(0)

    /**
     * Stores the current page in the general progress of configuration of the subscription.
     */
    val page = mutableStateOf(0)

    val isValid = mutableStateOf(false)

    /**
     * A result launcher that asks the user to pick a file to subscribe to.
     * @see fileUri
     */
    private var filePicker: ActivityResultLauncher<Array<String>>? = null

    /**
     * Initializes all the features of the view model that require an activity.
     * @param activity The activity that will be doing all the calls to the ViewModel.
     */
    fun initialize(activity: AppCompatActivity) {
        filePicker =
            activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) {
                    // keep the picked file accessible after the first sync and reboots
                    activity.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    fileUri.value = uri
                    fileName.value = activity.contentResolver
                        // Query the uri from the activity's content resolver
                        .query(uri, null, null, null, null)
                        // Move the pointer to the first result
                        ?.takeIf { it.moveToFirst() }
                        ?.use { cursor ->
                            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (index < 0) return@use null
                            cursor.getString(index)
                        }
                }
                isValid.value = fileUri.value != null
            }
    }

    /**
     * Requests the user to pick a file, and updates [fileUri] accordingly.
     * @see fileUri
     * @throws ActivityNotFoundException If there was no Activity found to run the given Intent.
     * @throws NullPointerException If [initialize] has not been called with the calling activity.
     */
    fun pickFile() {
        filePicker!!.launch(CALENDAR_MIME_TYPES)
    }

    /**
     * Resets all the values introduced into the form.
     */
    fun dispose() {
        url.value = ""
        urlError.value = null
        insecureUrlWarning.value = false

        requiresAuth.value = false
        username.value = ""
        password.value = ""

        fileUri.value = null
        fileName.value = null

        isValid.value = false
    }

    @UiThread
    fun updateUrl(url: String) {
        val uri = validateUri(url)
        this.url.value = uri?.toString() ?: url
        isValid.value = uri?.let { true } ?: false
    }

    /**
     * Starts the validation of the currently selected source.
     */
    fun validation(
        onSuccess: () -> Unit,
        onError: (exception: Exception) -> Unit,
    ) = viewModelScope.launch(Dispatchers.IO) {
        validate { info ->
            val exception = info.exception
            if (exception != null) {
                // There has been an error
                onError(exception)
            } else {
                // There are no errors, everything is fine
                info.calendarColor?.let { color.value = Color(it) }
                info.calendarName?.let { displayName.value = it }
                uri.value = info.uri
                onSuccess()
            }
        }
    }

    /**
     * Checks that the given url is valid, or shows an error message through [urlError] otherwise.
     * @param url The url to check for.
     * @return A parsed [Uri] from the given [url] or `null` if [url] is not valid.
     */
    private fun validateUri(url: String): Uri? {
        var errorMsg: String? = null

        var uri: Uri
        try {
            try {
                uri = Uri.parse(url)
            } catch (e: URISyntaxException) {
                Log.d(TAG, "Invalid URL", e)
                errorMsg = e.localizedMessage
                return null
            }

            Log.i(TAG, uri.toString())

            if (uri.scheme.equals("webcal", true)) {
                uri = uri.buildUpon().scheme("http").build()
                this.url.value = uri.toString()
                return null
            } else if (uri.scheme.equals("webcals", true)) {
                uri = uri.buildUpon().scheme("https").build()
                this.url.value = uri.toString()
                return null
            }

            val supportsAuthenticate = HttpUtils.supportsAuthentication(uri)
            when (uri.scheme?.lowercase()) {
                "content" -> {
                    // SAF file, no need for auth
                }
                "http", "https" -> {
                    // check whether the URL is valid
                    try {
                        uri.toString().toHttpUrl()
                    } catch (e: IllegalArgumentException) {
                        Log.w(TAG, "Invalid URI", e)
                        errorMsg = e.localizedMessage
                        return null
                    }

                    // extract user name and password from URL
                    uri.userInfo?.let { userInfo ->
                        val credentials = userInfo.split(':')
                        this.requiresAuth.value = true
                        username.value = credentials.elementAtOrNull(0) ?: username.value
                        password.value = credentials.elementAtOrNull(1) ?: password.value

                        val urlWithoutPassword =
                            URI(uri.scheme, null, uri.host, uri.port, uri.path, uri.query, null)
                        this.url.value = urlWithoutPassword.toString()
                        return null
                    }
                }
                else -> {
                    errorMsg = getString(R.string.add_calendar_need_valid_uri)
                    return null
                }
            }

            // warn if auth. required and not using HTTPS
            insecureUrlWarning.value = requiresAuth.value && !uri.scheme.equals("https", true)
        } finally {
            urlError.value = errorMsg
        }
        return uri
    }

    /**
     * Validates that the given uri is reachable and valid.
     */
    private suspend fun validate(@UiThread callback: suspend CoroutineScope.(info: ResourceInfo) -> Unit) {
        val uri = when (selectionType.value) {
            0 -> Uri.parse(url.value)
            else -> fileUri.value
        }
            ?: throw IllegalStateException("There is no valid uri stored. Type=${selectionType.value}, url=${url.value}, file=${fileUri.value}")
        Log.i(TAG, "Validating Webcal feed $uri (authentication: $username)")

        val info = ResourceInfo(uri)
        val downloader = object : CalendarFetcher(getApplication(), uri) {
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
                        properties[net.fortuna.ical4j.model.property.Color.PROPERTY_NAME]?.let { colorValue ->
                            Css3Color.colorFromString(colorValue)
                        } ?:
                                // try X-APPLE-CALENDAR-COLOR second
                                try {
                                    properties[ICalendar.CALENDAR_COLOR]?.let { colorValue ->
                                        Css3Color.colorFromString(colorValue)
                                    }
                                } catch (e: IllegalArgumentException) {
                                    Log.w(TAG, "Couldn't parse calendar COLOR", e)
                                    null
                                }
                    info.eventsFound = events.size
                }

                runBlocking(Dispatchers.Main) { callback(info) }
            }

            override suspend fun onNewPermanentUrl(target: Uri) {
                Log.i(TAG, "Got permanent redirect when validating, saving new URL: $target")
                val location = uri.toURI().resolve(target.toURI())
                info.uri = location.toUri()
            }

            override suspend fun onError(error: Exception) {
                Log.e(TAG, "Couldn't validate calendar", error)
                info.exception = error
                runBlocking(Dispatchers.Main) { callback(info) }
            }
        }

        downloader.username = username.value
        downloader.password = password.value

        // directly ask for confirmation of custom certificates
        downloader.inForeground = true

        downloader.fetch()
    }
}
