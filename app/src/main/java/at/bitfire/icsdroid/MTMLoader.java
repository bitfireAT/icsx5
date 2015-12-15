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

package at.bitfire.icsdroid;

import android.content.Context;
import android.util.Log;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import de.duenndns.ssl.MemorizingTrustManager;

public class MTMLoader {
    private static final String TAG = "ICSdroid.MTMLoader";

    public static void prepareHttpsURLConnection(Context context, HttpsURLConnection connection) {
        try {
            MemorizingTrustManager mtm = new MemorizingTrustManager(context);

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new X509TrustManager[] { mtm }, null);

            connection.setSSLSocketFactory(sc.getSocketFactory());
            connection.setHostnameVerifier(mtm.wrapHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier()));
        } catch (NoSuchAlgorithmException|KeyManagementException e) {
            Log.e(TAG, "Couldn't initialize MemorizingTrustManager", e);
        }
    }

}
