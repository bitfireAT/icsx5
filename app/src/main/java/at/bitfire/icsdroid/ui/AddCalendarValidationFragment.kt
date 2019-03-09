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
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import at.bitfire.ical4android.Event
import at.bitfire.icsdroid.CalendarFetcher
import at.bitfire.icsdroid.R
import okhttp3.MediaType
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL

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

            val errorMessage = info.exception?.localizedMessage
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

            val downloader = object: CalendarFetcher(originalUrl) {
                override fun onSuccess(data: InputStream, contentType: MediaType?) {
                    InputStreamReader(data, contentType?.charset() ?: Charsets.UTF_8).use { reader ->
                        val properties = mutableMapOf<String, String>()
                        val events = Event.fromReader(reader, properties)

                        // URL may have changed because of redirects
                        info.url = url

                        info.calendarName = properties[Event.CALENDAR_NAME]
                        info.eventsFound = events.size
                    }

                    postValue(info)
                }

                override fun onError(error: Exception) {
                    info.exception = error
                }
            }
            Thread(downloader).start()
        }

    }

}
