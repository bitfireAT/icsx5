/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Stores the credentials to be used with a specific subscription.
 */
@Entity(
    tableName = "credentials",
    foreignKeys = [
        ForeignKey(entity = Subscription::class, parentColumns = ["id"], childColumns = ["subscriptionId"], onDelete = ForeignKey.CASCADE),
    ]
)
data class Credential(
    @PrimaryKey val subscriptionId: Long,
    val username: String,
    val password: String
)