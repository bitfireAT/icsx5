package at.bitfire.icsdroid.utils

import org.json.JSONArray
import org.json.JSONException
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

/** Converts the [Iterable] into a [JSONArray]. */
fun Iterable<JSONObject>.toJSONArray(): JSONArray = JSONArray().apply {
    for (item in this@toJSONArray)
        put(item)
}

/**
 * Converts the array into a list of [JSONObject].
 * @throws JSONException If an element of the array is not a [JSONObject], or an error has occurred
 * in the conversion.
 */
fun JSONArray.mapJSONObjects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
