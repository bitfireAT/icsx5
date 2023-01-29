package at.bitfire.icsdroid

import android.net.Uri
import android.provider.CalendarContract
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.utils.serialization.JsonSerializable
import at.bitfire.icsdroid.utils.serialization.JsonSerializer
import org.junit.Assert
import org.junit.Test

class JsonParsingTest {
    private val conversions: Set<Pair<JsonSerializable, JsonSerializer<*>>> = setOf(
        Credential(
            1000,
            "testingUsername",
            "testingPassword",
        ) to Credential.Companion,
        Subscription(
            1000,
            Uri.parse("https://example.com"),
        null,
            "testingSubscription",
            "LocalCalendarTest",
            CalendarContract.ACCOUNT_TYPE_LOCAL,
            0L,
            0L,
            false,
            null,
            false,
            null,
            Subscription.DEFAULT_COLOR,
            isSynced = true,
            isVisible = true,
        ) to Subscription.Companion,
    )

    @Test
    fun testSerializableToJsonAndBack() {
        for ((serializable, serializer) in conversions) {
            val json = serializable.toJSON()
            val obj = serializer.fromJSON(json)
            Assert.assertEquals(serializable, obj)
        }
    }
}