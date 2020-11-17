/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import at.bitfire.icsdroid.BR
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.databinding.CredentialsBinding
import kotlinx.android.synthetic.main.credentials.view.*

class CredentialsFragment: Fragment() {

    val model by activityViewModels<CredentialsModel>()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
        val binding = DataBindingUtil.inflate<CredentialsBinding>(inflater, R.layout.credentials, container, false)
        binding.lifecycleOwner = this
        binding.setVariable(BR.model, model)

        val v = binding.root
        model.requiresAuth.observe(viewLifecycleOwner) { requiresAuth ->
            v.inputs.visibility = if (requiresAuth) View.VISIBLE else View.GONE
        }

        return v
    }

    class CredentialsModel : ViewModel() {
        var originalRequiresAuth: Boolean? = null
        var originalUsername: String? = null
        var originalPassword: String? = null

        val requiresAuth = MutableLiveData<Boolean>()
        val username = MutableLiveData<String>()
        val password = MutableLiveData<String>()

        init {
            requiresAuth.value = false
        }

        fun dirty() = requiresAuth.value != originalRequiresAuth ||
                username.value != originalUsername ||
                password.value != originalPassword
    }

}
