package at.bitfire.icsdroid.utils

import android.app.Application
import android.content.Context
import android.content.res.Resources.NotFoundException
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel

/**
 * Makes a new toast, and shows it.
 * @author Arnau Mora
 * @param textRes The resource string of the text to display.
 * @param duration The duration of the toast.
 * @return The toast being displayed.
 * @throws NotFoundException If the [textRes] specified could not be found.
 * @see Toast.LENGTH_SHORT
 * @see Toast.LENGTH_LONG
 */
@UiThread
fun Context.toast(@StringRes textRes: Int, duration: Int = Toast.LENGTH_SHORT): Toast =
    Toast.makeText(this, textRes, duration).also { it.show() }

/**
 * Makes a new toast, and shows it.
 * @author Arnau Mora
 * @param text The text to display.
 * @param duration The duration of the toast.
 * @return The toast being displayed.
 * @see Toast.LENGTH_SHORT
 * @see Toast.LENGTH_LONG
 */
@UiThread
fun Context.toast(text: String, duration: Int = Toast.LENGTH_SHORT): Toast =
    Toast.makeText(this, text, duration).also { it.show() }

/**
 * Makes a new toast, and shows it.
 * @author Arnau Mora
 * @param textRes The resource string of the text to display.
 * @param duration The duration of the toast.
 * @return The toast being displayed.
 * @throws NotFoundException If the [textRes] specified could not be found.
 * @throws IllegalArgumentException If not currently associated with a context.
 * @see Toast.LENGTH_SHORT
 * @see Toast.LENGTH_LONG
 */
@UiThread
fun Fragment.toast(@StringRes textRes: Int, duration: Int = Toast.LENGTH_SHORT): Toast =
    Toast.makeText(requireContext(), textRes, duration).also { it.show() }

/**
 * Makes a new toast, and shows it.
 * @author Arnau Mora
 * @param text The text to display.
 * @param duration The duration of the toast.
 * @return The toast being displayed.
 * @throws IllegalArgumentException If not currently associated with a context.
 * @see Toast.LENGTH_SHORT
 * @see Toast.LENGTH_LONG
 */
@UiThread
fun Fragment.toast(text: String, duration: Int = Toast.LENGTH_SHORT): Toast =
    Toast.makeText(requireContext(), text, duration).also { it.show() }

/**
 * Returns a localized formatted string from the application's package's default string table,
 * substituting the format arguments as defined in [java.util.Formatter] and [String.format].
 * @param resId Resource id for the format string
 * @param formatArgs The format arguments that will be used for substitution.
 * @return The string data associated with the resource, formatted and stripped of styled text
 * information.
 */
fun AndroidViewModel.getString(@StringRes resId: Int, vararg formatArgs: Any) =
    getApplication<Application>().getString(resId, formatArgs)
