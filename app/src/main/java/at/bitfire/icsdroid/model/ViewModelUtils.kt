package at.bitfire.icsdroid.model

import android.content.Context
import android.widget.Toast
import androidx.annotation.IntDef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Retention(AnnotationRetention.SOURCE)
@IntDef(Toast.LENGTH_SHORT, Toast.LENGTH_LONG)
annotation class ToastDuration

suspend fun toastAsync(
    context: Context,
    message: String?,
    cancelToast: Toast? = null,
    @ToastDuration duration: Int = Toast.LENGTH_LONG,
): Toast? = withContext(Dispatchers.Main) {
    cancelToast?.cancel()

    Toast.makeText(context, message, duration).also { it.show() }
}
