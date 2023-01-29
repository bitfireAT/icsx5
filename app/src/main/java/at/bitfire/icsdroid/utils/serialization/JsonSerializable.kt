package at.bitfire.icsdroid.utils.serialization

import org.json.JSONObject

/**
 * Indicates that a class can be serialized into JSON.
 */
interface JsonSerializable {
    /** Converts the class into a [JSONObject]. */
    fun toJSON(): JSONObject
}
