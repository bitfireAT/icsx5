/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R
import kotlinx.android.synthetic.main.add_calendar_enter_url.view.*
import java.net.URI
import java.net.URISyntaxException

class AddCalendarEnterUrlFragment: Fragment(), TextWatcher, CredentialsFragment.OnCredentialsChangeListener {

    companion object {
        val KEY_INFO = "info"
    }

    private lateinit var credentials: CredentialsFragment
    private var info = ResourceInfo()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            info = it.getSerializable(KEY_INFO) as ResourceInfo
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(KEY_INFO, info)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.add_calendar_enter_url, container, false)
        setHasOptionsMenu(true)

        credentials = CredentialsFragment()
        credentials.setOnChangeListener(this)
        childFragmentManager.beginTransaction()
                .replace(R.id.credentials, credentials)
                .commit()

        v.url.addTextChangedListener(this)
        activity.intent.data?.let {
            // This causes the onTextChanged listeners to be activated - credentials and insecureAuthWarning are already required!
            v.url.setText(it.toString())
        }

        return v
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.enter_url_fragment, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val itemNext = menu.findItem(R.id.next)
        val urlOK = info.url != null
        val authOK = !info.authRequired || (!info.username.isNullOrBlank() &&!info.password.isNullOrBlank())
        itemNext.isEnabled = urlOK && authOK

        val view = requireNotNull(view)
        if (!view.url.text.isNullOrEmpty() && !urlOK)
            view.url.error = getString(R.string.add_calendar_need_valid_uri)
    }


    /* dynamic changes */

    override fun onChangeCredentials(authRequired: Boolean, username: String?, password: String?) {
        info.authRequired = authRequired
        info.username = username
        info.password = password
        updateHttpWarning()
        activity.invalidateOptionsMenu()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        var uri: URI

        val view = requireNotNull(view)
        try {
            uri = URI(view.url.text.toString())
        } catch(e: URISyntaxException) {
            Log.d(Constants.TAG, "Invalid URL", e)
            return
        }

        if (uri.scheme.equals("webcal", true))
            uri = URI("http", uri.authority, uri.path, uri.query, null)
        else if (uri.scheme.equals("webcals", true))
            uri = URI("https", uri.authority, uri.path, uri.query, null)

        info.url = null
        try {
            when {
                uri.scheme.equals("file", true) && !uri.path.isNullOrBlank() -> {
                    info.url = uri.toURL()
                    credentials.authRequired = false
                    childFragmentManager.beginTransaction()
                            .hide(credentials)
                            .commit()
                }
                (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) && !uri.authority.isNullOrBlank() -> {
                    info.url = uri.toURL()
                    childFragmentManager.beginTransaction()
                            .show(credentials)
                            .commit()
                }
            }
        } catch(e: Exception) {
            Log.d(Constants.TAG, "Invalid URL", e)
            view.url.error = e.localizedMessage
        }
    }

    override fun afterTextChanged(s: Editable) {
        updateHttpWarning()
        activity.invalidateOptionsMenu()
    }

    private fun updateHttpWarning() {
        // warn if auth. required and not using HTTPS
        view!!.insecure_authentication_warning.visibility =
                if (info.authRequired && info.url != null && !info.url.protocol.equals("https", true))
                    View.VISIBLE
                else
                    View.GONE
    }


    /* actions */

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.next) {
            val frag = AddCalendarValidationFragment.newInstance(info)
            frag.show(fragmentManager, "validation")
            return true
        }
        return false
    }

}
