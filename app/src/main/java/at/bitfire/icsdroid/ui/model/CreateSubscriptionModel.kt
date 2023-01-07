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
import androidx.lifecycle.AndroidViewModel
import at.bitfire.icsdroid.Constants.TAG
import at.bitfire.icsdroid.HttpUtils
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.utils.getString
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.URI
import java.net.URISyntaxException

class CreateSubscriptionModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private val CALENDAR_MIME_TYPES = arrayOf("text/calendar")
    }

    val url = mutableStateOf("")
    val urlError = mutableStateOf<String?>(null)
    val insecureUrlWarning = mutableStateOf(false)

    val requiresAuth = mutableStateOf(false)
    val username = mutableStateOf("")
    val password = mutableStateOf("")

    val fileUri = mutableStateOf<Uri?>(null)
    val fileName = mutableStateOf<String?>(null)

    val currentPage = mutableStateOf(0)

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
}
