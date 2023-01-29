package at.bitfire.icsdroid.utils.serialization

import org.json.JSONException
import org.json.JSONObject

/**
 * Indicates this is a class that can serialize some JSON into another class. Usually used in
 * companion object as a Factory.
 */
interface JsonSerializer <T> {
    /**
     * Converts the given [json] into [T].
     * @throws JSONException If any problem occurs during the conversion.
     */
    fun fromJSON(json: JSONObject): T
}
