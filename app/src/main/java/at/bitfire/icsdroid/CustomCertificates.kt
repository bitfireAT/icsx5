/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid

import android.content.Context
import android.util.Log
import at.bitfire.cert4android.CustomCertManager
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext

object CustomCertificates {

    @JvmStatic
    fun prepareHttpsURLConnection(context: Context, connection: HttpsURLConnection, foreground: Boolean) {
        try {
            val manager = CustomCertManager(context, true)
            manager.appInForeground = foreground

            val sc = SSLContext.getInstance("TLS")
            sc.init(null, arrayOf(manager), null)

            connection.sslSocketFactory = sc.socketFactory
            connection.hostnameVerifier = manager.hostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier())
        } catch(e: Exception) {
            Log.e(Constants.TAG, "Couldn't initialize cert4android", e)
        }
    }

}
