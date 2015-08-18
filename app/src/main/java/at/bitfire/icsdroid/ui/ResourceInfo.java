package at.bitfire.icsdroid.ui;

import java.net.URL;

public class ResourceInfo {

    final URL url;

    Exception exception;

    int statusCode = -1;
    String statusMessage;

    int eventsFound = -1;

    ResourceInfo(URL url) {
        this.url = url;
    }

}
