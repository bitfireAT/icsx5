package at.bitfire.icsdroid

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import java.net.URISyntaxException

object UriUtils {
    /**
     * Starts the [Intent.ACTION_VIEW] intent with the given URL, if possible.
     * If the intent can't be resolved (for instance, because there is no browser
     * installed), this method does nothing.
     *
     * @param toastInstallBrowser whether to show "Please install a browser" toast when
     * the Intent could not be resolved
     *
     * @return true on success, false if the Intent could not be resolved (for instance, because
     * there is no user agent installed)
     */
    fun launchUri(
        context: Context,
        uri: Uri,
        action: String = Intent.ACTION_VIEW,
        toastInstallBrowser: Boolean = true
    ): Boolean {
        val intent = Intent(action, uri)
        try {
            context.startActivity(intent)
            return true
        } catch (e: ActivityNotFoundException) {
            // no browser available
        }

        if (toastInstallBrowser)
            Toast.makeText(context, R.string.install_browser, Toast.LENGTH_LONG).show()

        return false
    }

    /**
     * Tries to parse the given URL into an [Uri]. If it cannot be converted,`false` is returned,
     * otherwise `true`.
     */
    fun isValidUri(url: String): Boolean = try {
        Uri.parse(url)
        true
    } catch (e: URISyntaxException) {
        false
    }

    /**
     * Tries to extract the name of the file associated with the given [Uri]. May return null if the
     * uri doesn't have a valid [Uri.getPath].
     */
    fun Uri.getFileName(context: Context): String? {
        var result: String? = null
        if (scheme == "content") {
            context.contentResolver
                .query(this, null, null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) {
                            result = cursor.getString(index)
                        }
                    }
                }
        }
        return result ?: path?.substringAfterLast('/')
    }
}