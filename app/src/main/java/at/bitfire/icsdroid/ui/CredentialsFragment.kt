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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import at.bitfire.icsdroid.R
import kotlinx.android.synthetic.main.credentials.view.*

class CredentialsFragment: Fragment(), CompoundButton.OnCheckedChangeListener, TextWatcher {

    companion object {
        val ARG_AUTH_REQUIRED = "auth_required"
        val ARG_USERNAME = "username"
        val ARG_PASSWORD = "password"
    }

    var authRequired = false
    var username: String? = null
    var password: String? = null

    private var onChangeListener: OnCredentialsChangeListener? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.credentials, container, false)

        if (savedInstanceState == null && arguments != null) {
            val args = arguments
            authRequired = args.getBoolean(ARG_AUTH_REQUIRED)
            username = args.getString(ARG_USERNAME)
            password = args.getString(ARG_PASSWORD)
        } else if (savedInstanceState != null) {
            authRequired = savedInstanceState.getBoolean(ARG_AUTH_REQUIRED)
            username = savedInstanceState.getString(ARG_USERNAME)
            password = savedInstanceState.getString(ARG_PASSWORD)
        }

        v.requires_authentication.isChecked = authRequired
        v.requires_authentication.setOnCheckedChangeListener(this)

        v.user_name.setText(username)
        v.user_name.addTextChangedListener(this)

        v.password.setText(password)
        v.password.addTextChangedListener(this)

        updateViews()
        return v
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(ARG_AUTH_REQUIRED, authRequired)
        outState.putString(ARG_USERNAME, username)
        outState.putString(ARG_PASSWORD, password)
    }


    interface OnCredentialsChangeListener {
        fun onChangeCredentials(authRequired: Boolean, username: String?, password: String?)
    }

    fun setOnChangeListener(listener: OnCredentialsChangeListener) {
        onChangeListener = listener
    }

    private fun notifyListener() {
        onChangeListener?.onChangeCredentials(authRequired,
                if (authRequired) username else null, if (authRequired) password else null)
    }

    private fun updateViews() {
        view?.let { v ->
            if (authRequired) {
                v.user_name_label.visibility = View.VISIBLE
                v.user_name.visibility = View.VISIBLE
                v.password_label.visibility = View.VISIBLE
                v.password.visibility = View.VISIBLE
            } else {
                v.user_name_label.visibility = View.GONE
                v.user_name.visibility = View.GONE
                v.password_label.visibility = View.GONE
                v.password.visibility = View.GONE
            }
        }
    }


    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        authRequired = isChecked
        updateViews()
        notifyListener()
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable) {
        username = view!!.user_name.text.toString()
        password = view!!.password.text.toString()
        notifyListener()
    }

}
