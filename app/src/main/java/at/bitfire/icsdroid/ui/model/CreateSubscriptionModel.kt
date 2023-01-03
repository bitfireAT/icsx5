package at.bitfire.icsdroid.ui.model

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.annotation.UiThread
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
    val url = mutableStateOf("")
    val urlError = mutableStateOf<String?>(null)

    val requiresAuth = mutableStateOf(false)
    val username = mutableStateOf("")
    val password = mutableStateOf("")

    val currentPage = mutableStateOf(0)

    val isValid = mutableStateOf(false)

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

                        val urlWithoutPassword = URI(uri.scheme, null, uri.host, uri.port, uri.path, uri.query, null)
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
            /*binding.insecureAuthenticationWarning.visibility =
                if (credentialsModel.requiresAuth.value == true && !uri.scheme.equals("https", true))
                    View.VISIBLE
                else
                    View.GONE*/
        } finally {
            urlError.value = errorMsg
        }
        return uri
    }
}
