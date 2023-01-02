package at.bitfire.icsdroid.utils

import android.content.Intent
import android.os.Build
import android.os.Bundle
import kotlin.reflect.KClass

/**
 * Uses the correct method of [Bundle.getSerializable] according to the current SDK level.
 * @author Arnau Mora
 * @param key A String, or null
 * @param kClass The expected class of the returned type
 * @return A Serializable value, or null
 */
@Suppress("DEPRECATION", "UNCHECKED_CAST")
fun <T : java.io.Serializable> Bundle.getSerializableCompat(key: String?, kClass: KClass<T>): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        getSerializable(key, kClass.java)
    else
        getSerializable(key) as? T?

/**
 * Uses the correct method of [Intent.getSerializableExtra] according to the current SDK level.
 * @author Arnau Mora
 * @param key A String, or null
 * @param kClass The expected class of the returned type
 * @return A Serializable value, or null
 */
@Suppress("DEPRECATION", "UNCHECKED_CAST")
fun <T : java.io.Serializable> Intent.getSerializableCompat(key: String?, kClass: KClass<T>): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        getSerializableExtra(key, kClass.java)
    else
        getSerializableExtra(key) as? T?
