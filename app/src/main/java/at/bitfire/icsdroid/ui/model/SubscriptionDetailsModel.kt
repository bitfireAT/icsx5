package at.bitfire.icsdroid.ui.model

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
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

    open fun dispose() {
        requiresAuth.value = false
        username.value = ""
        password.value = ""

        color.value = Color(Subscription.DEFAULT_COLOR)
        displayName.value = ""
        uri.value = null
    }
}