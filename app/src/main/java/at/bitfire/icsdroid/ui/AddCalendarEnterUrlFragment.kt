/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.HttpUtils
import at.bitfire.icsdroid.HttpUtils.toUri
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.databinding.AddCalendarEnterUrlBinding
import java.net.URI
import java.net.URISyntaxException

class AddCalendarEnterUrlFragment: Fragment() {

    private val titleColorModel by activityViewModels<TitleColorFragment.TitleColorModel>()
    private val credentialsModel by activityViewModels<CredentialsFragment.CredentialsModel>()
    private lateinit var binding: AddCalendarEnterUrlBinding

    val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            // keep the picked file accessible after the first sync and reboots
            requireActivity().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            binding.url.editText?.setText(uri.toString())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
        val invalidate = Observer<Any> {
            requireActivity().invalidateOptionsMenu()
        }
        arrayOf(
                titleColorModel.url,
                credentialsModel.requiresAuth,
                credentialsModel.username,
                credentialsModel.password
        ).forEach {
            it.observe(viewLifecycleOwner, invalidate)
        }

        binding = AddCalendarEnterUrlBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.model = titleColorModel

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

    private fun validateUri(): URI? {
        var errorMsg: String? = null

        var uri: URI
        try {
            try {
                uri = URI(titleColorModel.url.value ?: return null)
            } catch (e: URISyntaxException) {
                Log.d(Constants.TAG, "Invalid URL", e)
                errorMsg = e.localizedMessage
                return null
            }

            Log.i(Constants.TAG, uri.toString())

            if (uri.scheme.equals("webcal", true)) {
                uri = URI("http", uri.authority, uri.path, uri.query, null)
                titleColorModel.url.value = uri.toString()
                return null
            } else if (uri.scheme.equals("webcals", true)) {
                uri = URI("https", uri.authority, uri.path, uri.query, null)
                titleColorModel.url.value = uri.toString()
                return null
            }

            val supportsAuthenticate = HttpUtils.supportsAuthentication(uri.toUri())
            binding.credentials.visibility = if (supportsAuthenticate) View.VISIBLE else View.GONE
            when (uri.scheme?.lowercase()) {
                "content" -> {
                    // SAF file, no need for auth
                }
                "http", "https" -> {
                    // extract user name and password from URL
                    uri.userInfo?.let { userInfo ->
                        val credentials = userInfo.split(':')
                        credentialsModel.username.value = credentials.elementAtOrNull(0)
                        credentialsModel.password.value = credentials.elementAtOrNull(1)

                        val urlWithoutPassword = URI(uri.scheme, null, uri.host, uri.port, uri.path, uri.query, null)
                        titleColorModel.url.value = urlWithoutPassword.toString()
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
            AddCalendarValidationFragment().show(parentFragmentManager, "validation")
            return true
        }
        return false
    }

}
