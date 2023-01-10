package at.bitfire.icsdroid.ui.model

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import at.bitfire.icsdroid.Constants.TAG
import at.bitfire.icsdroid.db.entity.Subscription

/**
 * Provides some functions and variables for screens that require editing or viewing the contents of
 * a subscription.
 */
open class SubscriptionDetailsModel(application: Application): AndroidViewModel(application) {
    val uri = mutableStateOf<Uri?>(null)

    val requiresAuth = mutableStateOf(false)
    val username = mutableStateOf("")
    val password = mutableStateOf("")

    val displayName = mutableStateOf("")
    val color = mutableStateOf(Color(Subscription.DEFAULT_COLOR))

    val ignoreEmbeddedAlerts = mutableStateOf(false)
    val defaultAlarm = mutableStateOf<Long?>(null)

    private lateinit var contentResolver: ContentResolver

    open fun initialize(activity: AppCompatActivity) {
        contentResolver = activity.contentResolver
    }

    open fun dispose() {
        requiresAuth.value = false
        username.value = ""
        password.value = ""

        color.value = Color(Subscription.DEFAULT_COLOR)
        displayName.value = ""
        uri.value = null
    }

    fun getUriDisplayName() = uri.value?.let { uri ->
        Log.v(TAG, "Getting display name for uri: $uri")
        if (uri.scheme?.startsWith("http", true) == true) {
            // If the uri is an url, return as is
            uri.toString()
        } else {
            // Otherwise, it's a file, try to get the file name
            Log.v(TAG, "Querying file display name...")
            contentResolver.query(uri, null, null, null, null)
                // Move the pointer to the first result
                ?.takeIf { it.moveToFirst() }
                ?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index < 0) return@use null
                    cursor.getString(index)
                }
        }
    }
}