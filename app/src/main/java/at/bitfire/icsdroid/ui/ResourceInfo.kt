/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui;

import java.net.URL

class ResourceInfo {

    var url: URL? = null

    var exception: Exception? = null

    var statusCode = -1
    var statusMessage: String? = null

    var calendarName: String? = null
    var eventsFound = -1

}
