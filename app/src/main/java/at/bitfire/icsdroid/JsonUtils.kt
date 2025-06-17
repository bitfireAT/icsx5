package at.bitfire.icsdroid

import org.json.JSONObject

/**
 * Returns the value mapped by name if it exists, coercing it if necessary, or `null` if no such mapping exists.
 */
fun JSONObject.getStringOrNull(name: String): String? {
    if (!has(name)) return null
    return getString(name)
}
