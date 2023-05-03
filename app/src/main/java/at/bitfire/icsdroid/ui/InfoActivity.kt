/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R
import com.google.accompanist.themeadapter.material.MdcTheme
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer

class InfoActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, LibsFragment())
                .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.app_info_activity, menu)
        return true
    }

    fun showWebSite(item: MenuItem) {
        launchUri(Uri.parse("https://icsx5.bitfire.at/?pk_campaign=icsx5-app&pk_kwd=info-activity"))
    }

    fun showTwitter(item: MenuItem) {
        launchUri(Uri.parse("https://twitter.com/icsx5app"))
    }

    private fun launchUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(Constants.TAG, "No browser installed")
        }
    }

    class LibsFragment: Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View = ComposeView(requireContext()).apply {
            setContent {
                MdcTheme {
                    LibrariesContainer(
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
