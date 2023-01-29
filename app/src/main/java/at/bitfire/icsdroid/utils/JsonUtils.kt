package at.bitfire.icsdroid.utils

import org.json.JSONObject

/**
 * Gets the object stored at [key] as a [String], or `null` if there's anything stored at that key,
 * or it's [JSONObject.NULL].
 * @param key The key where the value to get is stored at.
 */
fun JSONObject.getStringOrNull(key: String): String? =
    if (has(key) && !isNull(key))
        getString(key)
    else
        null

/**
 * Gets the object stored at [key] as a [Long], or `null` if there's anything stored at that key,
 * or it's [JSONObject.NULL].
 * @param key The key where the value to get is stored at.
 */
fun JSONObject.getLongOrNull(key: String): Long? =
    if (has(key) && !isNull(key))
        getLong(key)
    else
        null

/**
 * Gets the object stored at [key] as a [Int], or `null` if there's anything stored at that key,
 * or it's [JSONObject.NULL].
 * @param key The key where the value to get is stored at.
 */
fun JSONObject.getIntOrNull(key: String): Int? =
    if (has(key) && !isNull(key))
        getInt(key)
    else
        null
