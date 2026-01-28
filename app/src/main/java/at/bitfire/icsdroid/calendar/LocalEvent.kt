/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.calendar

import at.bitfire.ical4android.AndroidEvent

class LocalEvent(
    val androidEvent: AndroidEvent
) {
    fun add() = androidEvent.add()
}