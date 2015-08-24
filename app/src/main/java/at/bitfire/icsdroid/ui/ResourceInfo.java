/*
 * Copyright (c) 2013 – 2015 Ricki Hirner (bitfire web engineering).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 */

package at.bitfire.icsdroid.ui;

import java.net.URL;

public class ResourceInfo {

    final URL url;
    final boolean authRequired;
    final String username;
    final String password;

    Exception exception;

    int statusCode = -1;
    String statusMessage;

    int eventsFound = -1;

    ResourceInfo(URL url, boolean authRequired, String username, String password) {
        this.url = url;
        this.authRequired = authRequired;
        this.username = username;
        this.password = password;
    }

}
