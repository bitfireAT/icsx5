package at.bitfire.icsdroid.db

import android.net.Uri
import androidx.room.TypeConverter

/**
 * Provides converters for complex types in the Room DB.
 */
class Converters {
    /** Converts an [Uri] to a [String]. */
    @TypeConverter
    fun fromUri(value: Uri?): String? = value?.toString()

    /** Converts a [String] to an [Uri]. */
    @TypeConverter
    fun toUri(value: String?): Uri? = value?.let { Uri.parse(it) }
}