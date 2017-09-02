/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui;

import java.io.Serializable;
import java.net.URL;

public class ResourceInfo implements Serializable {

    URL url;
    boolean authRequired;
    String username;
    String password;

    Exception exception;

    int statusCode = -1;
    String statusMessage;

    String calendarName;
    int eventsFound = -1;

}
