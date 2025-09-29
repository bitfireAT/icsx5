package at.bitfire.icsdroid.model

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun toastAsync(
    context: Context,
    message: String?,
    cancelToast: Toast? = null,
    duration: Int = Toast.LENGTH_LONG,
): Toast? = withContext(Dispatchers.Main) {
    cancelToast?.cancel()

    Toast.makeText(context, message, duration).also { it.show() }
}
