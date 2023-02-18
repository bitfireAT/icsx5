package at.bitfire.icsdroid

import android.content.Intent
import android.os.Build
import android.os.Bundle
import java.io.Serializable

/**
 * Retrieve extended data from the intent.
 * @param name The name of the desired item.
 * @return The value of an item previously added with [Intent.putExtra], or `null` if no
 * [Serializable] value was found.
 * @see Intent.putExtra
 */
@Suppress("UNCHECKED_CAST", "DEPRECATION")
inline fun <reified T: Serializable> Intent.getSerializableExtraCompat(name: String): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        getSerializableExtra(name, T::class.java)
    else
        getSerializableExtra(name) as? T?

/**
 * Returns the value associated with the given key, or `null` if no mapping of the desired type
 * exists for the given key or a null value is explicitly associated with the key.
 * @param key A String, or null
 * @return A Serializable value, or null
 */
@Suppress("UNCHECKED_CAST", "DEPRECATION")
inline fun <reified T: Serializable> Bundle.getSerializableCompat(key: String?): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        getSerializable(key, T::class.java)
    else
        getSerializable(key) as? T?
