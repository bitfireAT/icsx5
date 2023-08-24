package at.bitfire.icsdroid

import android.content.Context
import android.content.Intent

object IntentUtils {
    /**
     * Launches an intent for sharing the given [text].
     *
     * @param text The text to be shared.
     * @param title If any, the title to show in the share sheet.
     *
     * @return The intent used for sharing. Has already been launched, no need to [Context.startActivity].
     */
    fun Context.share(
        text: String,
        title: String? = null
    ): Intent {
        val intent = Intent().apply {
            this.action = Intent.ACTION_SEND
            this.type = "text/plain"

            putExtra(Intent.EXTRA_TEXT, text)
        }
        val shareIntent = Intent.createChooser(intent, title)

        return shareIntent.also { startActivity(it) }
    }
}
