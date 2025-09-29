package at.bitfire.icsdroid.model

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun toastAsync(
    context: Context,
    cancelToast: Toast? = null,
    duration: Int = Toast.LENGTH_LONG,
    message: Context.() -> String?
): Toast? = withContext(Dispatchers.Main) {
    cancelToast?.cancel()

    val msg = message(context)
    Toast.makeText(context, msg, duration).also { it.show() }
}
