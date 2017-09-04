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
import java.net.URL

class AddCalendarEnterUrlFragment: Fragment(), TextWatcher, CredentialsFragment.OnCredentialsChangeListener {

    private lateinit var credentials: CredentialsFragment

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
        val v = inflater.inflate(R.layout.add_calendar_enter_url, container, false)
        setHasOptionsMenu(true)

        credentials = childFragmentManager.findFragmentById(R.id.credentials) as? CredentialsFragment ?: {
            val frag = CredentialsFragment()
            frag.setOnChangeListener(this)
            childFragmentManager.beginTransaction()
                    .replace(R.id.credentials, frag)
                    .commit()
            frag
        }()

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
        val v = requireNotNull(view)

        val itemNext = menu.findItem(R.id.next)
        val urlOK = try {
            val uri = URI(v.url.text.toString())
            uri.scheme.equals("file", true) || uri.scheme.equals("http", true) || uri.scheme.equals("https", true)
        } catch(e: URISyntaxException) {
            false
        }
        if (v.url.text.isNotEmpty() && !urlOK)
            v.url.error = getString(R.string.add_calendar_need_valid_uri)

        val authOK = !credentials.requiresAuth || (!credentials.username.isNullOrBlank() && !credentials.password.isNullOrBlank())

        itemNext.isEnabled = urlOK && authOK
    }


    /* dynamic changes */

    override fun onChangeCredentials(username: String?, password: String?) {
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

        try {
            when {
                uri.scheme.equals("file", true) && !uri.path.isNullOrBlank() -> {
                    credentials.requiresAuth = false
                    childFragmentManager.beginTransaction()
                            .hide(credentials)
                            .commit()
                }
                (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) && !uri.authority.isNullOrBlank() -> {
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
        val url = view!!.url.text.toString()
        view!!.insecure_authentication_warning.visibility =
                if (credentials.requiresAuth && !url.startsWith("https", true))
                    View.VISIBLE
                else
                    View.GONE
    }


    /* actions */

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.next) {
            val info = ResourceInfo()
            info.url = URL(view!!.url.text.toString())
            if (credentials.requiresAuth) {
                info.username = credentials.username
                info.password = credentials.password
            }
            val frag = AddCalendarValidationFragment.newInstance(info)
            frag.show(fragmentManager, "validation")
            return true
        }
        return false
    }

}
