/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui

import android.app.Application
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import at.bitfire.cert4android.CustomCertManager
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
import kotlin.concurrent.thread

class AddCalendarValidationFragment: DialogFragment() {

    private lateinit var titleColorModel: TitleColorFragment.TitleColorModel
    private lateinit var credentialsModel: CredentialsFragment.CredentialsModel
    private lateinit var validationModel: ValidationModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        titleColorModel = ViewModelProviders.of(requireActivity()).get(TitleColorFragment.TitleColorModel::class.java)
        credentialsModel = ViewModelProviders.of(requireActivity()).get(CredentialsFragment.CredentialsModel::class.java)

        validationModel = ViewModelProviders.of(requireActivity()).get(ValidationModel::class.java)
        val url = URL(titleColorModel.url.value ?: throw IllegalArgumentException("No URL given"))
        validationModel.results(url, credentialsModel.username.value, credentialsModel.password.value).observe(this, Observer { info ->
            dialog.dismiss()

            val errorMessage = when {
                info.exception != null ->
                    info.exception?.message
                info.statusCode != HttpURLConnection.HTTP_OK ->
                    "${info.statusCode} ${info.statusMessage}"
                else -> null
            }

            if (errorMessage == null) {
                titleColorModel.url.value = info.url.toString()

                if (titleColorModel.title.value.isNullOrBlank())
                    titleColorModel.title.value = info.calendarName ?: info.url?.file

                requireFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, AddCalendarDetailsFragment())
                        .addToBackStack(null)
                        .commitAllowingStateLoss()
            } else
                Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT).show()
        })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val progress = ProgressDialog(activity)
        progress.setMessage(getString(R.string.add_calendar_validating))
        return progress
    }


    /* model and data source */

    class ValidationModel(
            application: Application
    ): AndroidViewModel(application) {

        fun results(url: URL, username: String?, password: String?) =
                CalendarSourceInfo(getApplication(), url, username, password)

    }

    class CalendarSourceInfo(
            context: Context,
            originalUrl: URL,
            username: String?,
            password: String?
    ): LiveData<ResourceInfo>() {

        init {
            val info = ResourceInfo()

            thread {
                info.exception = null
                var url = originalUrl

                var conn: URLConnection? = null
                var certManager: CustomCertManager? = null
                try {
                    var followRedirect: Boolean
                    var redirect = 0
                    do {
                        followRedirect = false
                        try {
                            conn = MiscUtils.prepareConnection(url)

                            if (conn is HttpsURLConnection) {
                                certManager = CustomCertificates.certManager(context, true)
                                CustomCertificates.prepareURLConnection(certManager, conn)
                            }

                            if (conn is HttpURLConnection) {
                                conn.instanceFollowRedirects = false

                                if (username != null && password != null) {
                                    val basicCredentials = "$username:$password"
                                    conn.setRequestProperty("Authorization", "Basic " + Base64.encodeToString(basicCredentials.toByteArray(), Base64.NO_WRAP))
                                }

                                info.statusCode = conn.responseCode
                                info.statusMessage = conn.responseMessage

                                // handle redirects
                                val location = conn.getHeaderField("Location")
                                if (info.statusCode/100 == 3 && location != null) {
                                    Log.i(Constants.TAG, "Following redirect to $location")
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
                        Log.e(Constants.TAG, "Couldn't parse iCalendar", e)
                    } finally {
                        (conn as? HttpURLConnection)?.disconnect()
                    }
                } finally {
                    certManager?.close()
                }

                info.url = url
                postValue(info)
            }
        }

    }

}
