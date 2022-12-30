package at.bitfire.icsdroid.utils

import android.content.Context
import android.content.res.Resources.NotFoundException
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment

/**
 * Makes a new toast, and shows it.
 * @author Arnau Mora
 * @since 20221230
 * @param textRes The resource string of the text to display.
 * @param duration The duration of the toast.
 * @return The toast being displayed.
 * @throws NotFoundException If the [textRes] specified could not be found.
 * @see Toast.LENGTH_SHORT
 * @see Toast.LENGTH_LONG
 */
@UiThread
@Throws(NotFoundException::class)
fun Context.toast(@StringRes textRes: Int, duration: Int = Toast.LENGTH_SHORT): Toast =
    Toast.makeText(this, textRes, duration).also { it.show() }

/**
 * Makes a new toast, and shows it.
 * @author Arnau Mora
 * @since 20221230
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
 * @since 20221230
 * @param textRes The resource string of the text to display.
 * @param duration The duration of the toast.
 * @return The toast being displayed.
 * @throws NotFoundException If the [textRes] specified could not be found.
 * @throws IllegalArgumentException If not currently associated with a context.
 * @see Toast.LENGTH_SHORT
 * @see Toast.LENGTH_LONG
 */
@UiThread
@Throws(NotFoundException::class, IllegalArgumentException::class)
fun Fragment.toast(@StringRes textRes: Int, duration: Int = Toast.LENGTH_SHORT): Toast =
    Toast.makeText(requireContext(), textRes, duration).also { it.show() }

/**
 * Makes a new toast, and shows it.
 * @author Arnau Mora
 * @since 20221230
 * @param text The text to display.
 * @param duration The duration of the toast.
 * @return The toast being displayed.
 * @throws IllegalArgumentException If not currently associated with a context.
 * @see Toast.LENGTH_SHORT
 * @see Toast.LENGTH_LONG
 */
@UiThread
@Throws(IllegalArgumentException::class)
fun Fragment.toast(text: String, duration: Int = Toast.LENGTH_SHORT): Toast =
    Toast.makeText(requireContext(), text, duration).also { it.show() }
