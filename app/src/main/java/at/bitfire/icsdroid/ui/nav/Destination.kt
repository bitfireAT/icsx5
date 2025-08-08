package at.bitfire.icsdroid.ui.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination : NavKey {
    @Serializable
    object SubscriptionList : Destination

    @Serializable
    data class EditSubscription(
        val subscriptionId: Long
    ) : Destination
}
