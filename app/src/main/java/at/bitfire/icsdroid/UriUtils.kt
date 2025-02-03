package at.bitfire.icsdroid

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

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
    fun launchUri(context: Context, uri: Uri, action: String = Intent.ACTION_VIEW, toastInstallBrowser: Boolean = true): Boolean {
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
     * Strips the URL from a string. For example, the following string:
     * ```
     * "This is a URL: https://example.com"
     * ```
     * will return:
     * ```
     * "https://example.com"
     * ```
     * _Quotes are not included_
     * @return The URL found in the string
     * @throws IllegalArgumentException if no URL is found in the string
     */
    fun String.stripUrl(): String? {
        //      schema             host             port         path               query
        return ("([a-zA-Z]+://)" + "([\\w\\-.]+)" + "(:\\d+)?" + "([\\w\\-./]+)?" + "(\\?[\\w.&=*]*)?")
            .toRegex()
            .find(this)
            ?.value
    }
}