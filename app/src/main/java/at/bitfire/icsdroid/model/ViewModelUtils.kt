package at.bitfire.icsdroid.model

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun toastAsync(
    context: Context,
    message: Context.() -> String? = { null },
    @StringRes messageResId: Int? = null,
    cancelToast: Toast? = null,
    duration: Int = Toast.LENGTH_SHORT
): Toast? = withContext(Dispatchers.Main) {
    cancelToast?.cancel()

    val msg = message(context)
    when {
        msg != null -> Toast.makeText(context, msg, duration)
        messageResId != null -> Toast.makeText(context, messageResId, duration)
        else -> return@withContext null
    }.also { it.show() }
}
