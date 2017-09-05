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
import org.apache.commons.lang3.StringUtils

class CredentialsFragment: Fragment(), CompoundButton.OnCheckedChangeListener, TextWatcher {

    companion object {
        val ARG_AUTH_REQUIRED = "auth_required"
        val ARG_USERNAME = "username"
        val ARG_PASSWORD = "password"

        fun newInstance(username: String? = null, password: String? = null): CredentialsFragment {
            val frag = CredentialsFragment()
            val args = Bundle(3)
            args.putBoolean(ARG_AUTH_REQUIRED, username != null || password != null)
            args.putString(ARG_USERNAME, username)
            args.putString(ARG_PASSWORD, password)
            frag.arguments = args
            return frag
        }

    }

    private var onChangeListener: OnCredentialsChangeListener? = null

    var requiresAuth
        get() = view?.requires_authentication?.isChecked ?: false
        set(value) {
            view?.requires_authentication?.isChecked = value
            updateViews()
        }

    var username: String?
        get() = StringUtils.trimToNull(view?.user_name?.text.toString())
        set(value) {
            view?.user_name?.setText(value)
            updateViews()
        }
    var password: String?
        get() = StringUtils.trimToNull(view?.password?.text.toString())
        set(value) {
            view?.password?.setText(value)
            updateViews()
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
        val v = inflater.inflate(R.layout.credentials, container, false)

        arguments?.let { args ->
            v.requires_authentication.isChecked = args.getBoolean(ARG_AUTH_REQUIRED)
            v.user_name.setText(args.getString(ARG_USERNAME))
            v.password.setText(args.getString(ARG_PASSWORD))
        }

        v.requires_authentication.setOnCheckedChangeListener(this)
        v.user_name.addTextChangedListener(this)
        v.password.addTextChangedListener(this)
        return v
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        updateViews()
    }

    interface OnCredentialsChangeListener {
        fun onChangeCredentials(username: String?, password: String?)
    }

    fun setOnChangeListener(listener: OnCredentialsChangeListener) {
        onChangeListener = listener
    }

    private fun notifyListener() {
        onChangeListener?.onChangeCredentials(if (requiresAuth) username else null, if (requiresAuth) password else null)
    }

    private fun updateViews() {
        view?.let { v ->
            if (v.requires_authentication.isChecked) {
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
        updateViews()
        notifyListener()
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable) {
        notifyListener()
    }

}
