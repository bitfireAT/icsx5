/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import at.bitfire.icsdroid.BR
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.databinding.AddCalendarEnterUrlBinding
import kotlinx.android.synthetic.main.add_calendar_enter_url.view.*
import java.net.URI
import java.net.URISyntaxException

class AddCalendarEnterUrlFragment: Fragment() {

    private lateinit var titleColorModel: TitleColorFragment.TitleColorModel
    private lateinit var credentialsModel: CredentialsFragment.CredentialsModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
        titleColorModel = ViewModelProviders.of(requireActivity()).get(TitleColorFragment.TitleColorModel::class.java)
        credentialsModel = ViewModelProviders.of(requireActivity()).get(CredentialsFragment.CredentialsModel::class.java)

        val invalidate = Observer<Any> {
            requireActivity().invalidateOptionsMenu()
        }
        arrayOf(
                titleColorModel.url,
                credentialsModel.requiresAuth,
                credentialsModel.username,
                credentialsModel.password
        ).forEach {
            it.observe(this, invalidate)
        }

        val binding = DataBindingUtil.inflate<AddCalendarEnterUrlBinding>(inflater, R.layout.add_calendar_enter_url, container, false)
        binding.lifecycleOwner = this
        binding.setVariable(BR.model, titleColorModel)

        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        validateUrl()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.enter_url_fragment, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val v = requireNotNull(view)
        val itemNext = menu.findItem(R.id.next)

        val url = validateUrl()

        val authOK = if (credentialsModel.requiresAuth.value == true)
                !credentialsModel.username.value.isNullOrEmpty() && !credentialsModel.password.value.isNullOrEmpty()
        else
            true

        val permOK = if (url?.scheme.equals("file", true))
            ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        else
            true

        itemNext.isEnabled = url != null && authOK && permOK
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.i(Constants.TAG, "Received request permissions! $requestCode")
        if (grantResults.contains(PackageManager.PERMISSION_GRANTED))
            requireActivity().invalidateOptionsMenu()
    }

    /* dynamic changes */

    private fun validateUrl(): URI? {
        val view = requireNotNull(view)

        var url: URI
        try {
            url = URI(titleColorModel.url.value ?: return null)
        } catch(e: URISyntaxException) {
            Log.d(Constants.TAG, "Invalid URL", e)
            view.url.error = e.localizedMessage
            return null
        }

        Log.i(Constants.TAG, url.toString())

        if (url.scheme.equals("webcal", true)) {
            url = URI("http", url.authority, url.path, url.query, null)
            titleColorModel.url.value = url.toString()
            return null
        } else if (url.scheme.equals("webcals", true)) {
            url = URI("https", url.authority, url.path, url.query, null)
            titleColorModel.url.value = url.toString()
            return null
        }

        when (url.scheme?.toLowerCase()) {
            "file" -> {
                if (url.path != null) {
                    // local file:
                    // 1. no need for auth
                    credentialsModel.requiresAuth.value = false
                    // 2. permission required
                    if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                        requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
                }
            }
            "http", "https" -> {
                // extract user name and password from URL
                url.userInfo?.let { userInfo ->
                    val credentials = userInfo.split(':')
                    credentialsModel.requiresAuth.value = true
                    credentialsModel.username.value = credentials.elementAtOrNull(0)
                    credentialsModel.password.value = credentials.elementAtOrNull(1)

                    val urlWithoutPassword = URI(url.scheme, null, url.host, url.port, url.path, url.query, null)
                    titleColorModel.url.value = urlWithoutPassword.toString()
                    return null
                }
            }
            else -> {
                view.url.error = getString(R.string.add_calendar_need_valid_uri)
                return null
            }
        }

        // warn if auth. required and not using HTTPS
        view.insecure_authentication_warning.visibility =
                if (credentialsModel.requiresAuth.value == true && !url.scheme.equals("https", true))
                    View.VISIBLE
                else
                    View.GONE

        return url
    }


    /* actions */

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.next) {
            AddCalendarValidationFragment().show(requireFragmentManager(), "validation")
            return true
        }
        return false
    }

}
