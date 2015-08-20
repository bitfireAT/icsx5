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
