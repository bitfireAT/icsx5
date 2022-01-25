/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import java.net.URL

data class ResourceInfo(

    var url: URL,

    var exception: Exception? = null,

    var calendarName: String? = null,
    var eventsFound: Int = 0

)