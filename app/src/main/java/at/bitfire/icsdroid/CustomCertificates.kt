/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid

import android.content.Context
import javax.net.ssl.HttpsURLConnection

object CustomCertificates {

    @JvmStatic
    fun prepareHttpsURLConnection(context: Context, connection: HttpsURLConnection) {
        /*try {
            MemorizingTrustManager mtm = new MemorizingTrustManager(context);

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new X509TrustManager[] { mtm }, null);

            connection.setSSLSocketFactory(sc.getSocketFactory());
            connection.setHostnameVerifier(mtm.wrapHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier()));
        } catch (NoSuchAlgorithmException|KeyManagementException e) {
            Log.e(TAG, "Couldn't initialize MemorizingTrustManager", e);
        }*/
    }

}
