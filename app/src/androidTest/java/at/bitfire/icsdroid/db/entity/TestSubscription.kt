package at.bitfire.icsdroid.db.entity

import androidx.core.net.toUri
import org.junit.Assert.assertEquals
import org.junit.Test

class TestSubscription {
    @Test
    fun test_json_conversion() {
        val subscription = Subscription(
            url = "".toUri(),
            displayName = "abc"
        )
        val json = subscription.toJSON()
        assertEquals(subscription, Subscription(json))
    }
}
