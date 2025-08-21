/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.ui.nav

import androidx.annotation.ColorInt
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination : NavKey {
    @Serializable
    object SubscriptionList : Destination

    @Serializable
    data class AddSubscription(
        val title: String? = null,
        @param:ColorInt val color: Int? = null,
    ): Destination

    @Serializable
    data class EditSubscription(
        val subscriptionId: Long
    ): Destination
}
