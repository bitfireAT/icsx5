/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.AsyncTaskLoader
import android.support.v4.content.Loader
import android.util.Base64
import android.util.Log
import android.widget.Toast
import at.bitfire.ical4android.Event
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.CustomCertificates
import at.bitfire.icsdroid.MiscUtils
import at.bitfire.icsdroid.R
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import javax.net.ssl.HttpsURLConnection

class AddCalendarValidationFragment: DialogFragment(), LoaderManager.LoaderCallbacks<ResourceInfo> {

    companion object {
        val ARG_INFO = "info"

        fun newInstance(info: ResourceInfo): AddCalendarValidationFragment {
            val frag = AddCalendarValidationFragment()
            val args = Bundle(1)
            args.putSerializable(ARG_INFO, info)
            frag.arguments = args
            return frag
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loaderManager.initLoader(0, arguments, this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val progress = ProgressDialog(activity)
        progress.setMessage(getString(R.string.add_calendar_validating))
        return progress
    }


    // loader callbacks

    override fun onCreateLoader(id: Int, args: Bundle?) =
            ResourceInfoLoader(activity, args?.getSerializable(ARG_INFO) as ResourceInfo)

    override fun onLoadFinished(loader: Loader<ResourceInfo>, info: ResourceInfo) {
        dialog.dismiss()

        val errorMessage = when {
            info.exception != null ->
                info.exception?.message
            info.statusCode != HttpURLConnection.HTTP_OK ->
                "${info.statusCode} ${info.statusMessage}"
            else -> null
        }

        if (errorMessage == null) {
            if (info.calendarName.isNullOrBlank())
                info.calendarName = info.url?.file

            fragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, AddCalendarDetailsFragment.newInstance(info))
                    .addToBackStack(null)
                    .commitAllowingStateLoss()
        } else
            Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT).show()
    }

    override fun onLoaderReset(loader: Loader<ResourceInfo>) {
    }


    // loader

    class ResourceInfoLoader(
            context: Context,
            val info: ResourceInfo
    ): AsyncTaskLoader<ResourceInfo>(context) {

        private var loaded = false

        override fun onStartLoading() {
            if (!loaded)
                forceLoad()
        }

        override fun loadInBackground(): ResourceInfo {
            info.exception = null

            var conn: URLConnection? = null

            var url = info.url
            var followRedirect: Boolean
            var redirect = 0
            do {
                followRedirect = false
                try {
                    conn = url!!.openConnection()
                    conn.connectTimeout = 7000
                    conn.readTimeout = 20000

                    if (conn is HttpsURLConnection)
                        CustomCertificates.prepareHttpsURLConnection(context, conn)

                    if (conn is HttpURLConnection) {
                        conn.setRequestProperty("User-Agent", Constants.USER_AGENT)
                        conn.instanceFollowRedirects = false

                        if (info.username != null && info.password != null) {
                            val basicCredentials = "${info.username}:${info.password}"
                            conn.setRequestProperty("Authorization", "Basic " + Base64.encodeToString(basicCredentials.toByteArray(), Base64.NO_WRAP))
                        }

                        info.statusCode = conn.responseCode
                        info.statusMessage = conn.responseMessage

                        // handle redirects
                        val location = conn.getHeaderField("Location")
                        if (info.statusCode/100 == 3 && location != null) {
                            Log.i(Constants.TAG, "Following redirect to " + location)
                            url = URL(url, location)
                            followRedirect = true
                            if (info.statusCode == HttpURLConnection.HTTP_MOVED_PERM) {
                                Log.i(Constants.TAG, "Permanent redirect: saving new location")
                                info.url = url
                            }
                        }

                        // only read stream if status is 200 OK
                        if (info.statusCode != HttpURLConnection.HTTP_OK) {
                            conn.disconnect()
                            conn = null
                        }

                    } else
                        // local file, always simulate HTTP status 200 OK
                        info.statusCode = HttpURLConnection.HTTP_OK

                } catch (e: IOException) {
                    info.exception = e
                }
            } while (followRedirect && redirect++ < Constants.MAX_REDIRECTS)

            try {
                conn?.let {
                    InputStreamReader(it.getInputStream(), MiscUtils.charsetFromContentType(it.contentType)).use { reader ->
                        val properties = mutableMapOf<String, String>()
                        val events = Event.fromReader(reader, properties)

                        info.calendarName = properties[Event.CALENDAR_NAME]
                        info.eventsFound = events.size
                    }
                }
            } catch(e: Exception) {
                info.exception = e
            } finally {
                (conn as? HttpURLConnection)?.disconnect()
            }

            loaded = true
            return info
        }
    }
}
