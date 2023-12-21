/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.HttpUtils
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.databinding.AddCalendarEnterUrlBinding
import at.bitfire.icsdroid.model.CredentialsModel
import java.net.URI
import java.net.URISyntaxException
import okhttp3.HttpUrl.Companion.toHttpUrl

class AddCalendarEnterUrlFragment: Fragment() {

    private val subscriptionSettingsModel by activityViewModels<SubscriptionSettingsFragment.SubscriptionSettingsModel>()
    private val credentialsModel by activityViewModels<CredentialsModel>()
    private lateinit var binding: AddCalendarEnterUrlBinding

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            // keep the picked file accessible after the first sync and reboots
            requireActivity().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            binding.url.editText?.setText(uri.toString())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
        val invalidate = Observer<Any?> {
            requireActivity().invalidateOptionsMenu()
        }
        arrayOf(
            subscriptionSettingsModel.url,
            credentialsModel.requiresAuth,
            credentialsModel.username,
            credentialsModel.password
        ).forEach {
            it.observe(viewLifecycleOwner, invalidate)
        }

        binding = AddCalendarEnterUrlBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.model = subscriptionSettingsModel

        binding.credentialsComposable.apply {
            // Dispose the Composition when viewLifecycleOwner is destroyed
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                LoginCredentialsComposable(
                    credentialsModel.requiresAuth.observeAsState(false).value,
                    credentialsModel.username.observeAsState("").value,
                    credentialsModel.password.observeAsState("").value,
                    onRequiresAuthChange = { credentialsModel.requiresAuth.postValue(it) },
                    onUsernameChange = { credentialsModel.username.postValue(it) },
                    onPasswordChange = { credentialsModel.password.postValue(it) },
                )
            }
        }

        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.pickStorageFile.setOnClickListener {
            pickFile.launch(arrayOf("text/calendar"))
        }

        validateUri()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.enter_url_fragment, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val itemNext = menu.findItem(R.id.next)

        val uri = validateUri()

        val authOK =
            if (credentialsModel.requiresAuth.value == true)
                !credentialsModel.username.value.isNullOrEmpty() && !credentialsModel.password.value.isNullOrEmpty()
            else
                true
        itemNext.isEnabled = uri != null && authOK
    }


    /* dynamic changes */

    private fun validateUri(): Uri? {
        var errorMsg: String? = null

        var uri: Uri
        try {
            try {
                uri = Uri.parse(subscriptionSettingsModel.url.value ?: return null)
            } catch (e: URISyntaxException) {
                Log.d(Constants.TAG, "Invalid URL", e)
                errorMsg = e.localizedMessage
                return null
            }

            Log.i(Constants.TAG, uri.toString())

            if (uri.scheme.equals("webcal", true)) {
                uri = uri.buildUpon().scheme("http").build()
                subscriptionSettingsModel.url.value = uri.toString()
                return null
            } else if (uri.scheme.equals("webcals", true)) {
                uri = uri.buildUpon().scheme("https").build()
                subscriptionSettingsModel.url.value = uri.toString()
                return null
            }

            val supportsAuthenticate = HttpUtils.supportsAuthentication(uri)
            binding.credentialsComposable.visibility = if (supportsAuthenticate) View.VISIBLE else View.GONE
            when (uri.scheme?.lowercase()) {
                "content" -> {
                    // SAF file, no need for auth
                }
                "http", "https" -> {
                    // check whether the URL is valid
                    try {
                        uri.toString().toHttpUrl()
                    } catch (e: IllegalArgumentException) {
                        Log.w(Constants.TAG, "Invalid URI", e)
                        errorMsg = e.localizedMessage
                        return null
                    }

                    // extract user name and password from URL
                    uri.userInfo?.let { userInfo ->
                        val credentials = userInfo.split(':')
                        credentialsModel.requiresAuth.value = true
                        credentialsModel.username.value = credentials.elementAtOrNull(0)
                        credentialsModel.password.value = credentials.elementAtOrNull(1)

                        val urlWithoutPassword = URI(uri.scheme, null, uri.host, uri.port, uri.path, uri.query, null)
                        subscriptionSettingsModel.url.value = urlWithoutPassword.toString()
                        return null
                    }
                }
                else -> {
                    errorMsg = getString(R.string.add_calendar_need_valid_uri)
                    return null
                }
            }

            // warn if auth. required and not using HTTPS
            binding.insecureAuthenticationWarning.visibility =
                    if (credentialsModel.requiresAuth.value == true && !uri.scheme.equals("https", true))
                        View.VISIBLE
                    else
                        View.GONE
        } finally {
            binding.url.error = errorMsg
        }
        return uri
    }


    /* actions */

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.next) {

            // flush the credentials if auth toggle is disabled
            if (credentialsModel.requiresAuth.value != true) {
                credentialsModel.username.value = null
                credentialsModel.password.value = null
            }

            AddCalendarValidationFragment().show(parentFragmentManager, "validation")
            return true
        }
        return false
    }

}
