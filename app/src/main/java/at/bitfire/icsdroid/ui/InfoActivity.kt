/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import at.bitfire.icsdroid.BuildConfig
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder

class InfoActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val builder = LibsBuilder()
                .withAboutIconShown(true)
                .withAboutAppName(getString(R.string.app_name))
                .withAboutDescription(getString(R.string.app_info_description))
                .withAboutVersionShownName(true)
                .withAboutVersionString(getString(R.string.app_info_version, BuildConfig.VERSION_NAME, BuildConfig.FLAVOR))
                .withAboutSpecial1(getString(R.string.app_info_gplv3))
                .withAboutSpecial1Description(getString(R.string.app_info_gplv3_note))
                .withLicenseShown(true)

                .withFields(R.string::class.java.fields)
                .withLibraryModification("org_brotli__dec", Libs.LibraryFields.LIBRARY_NAME, "Brotli")
                .withLibraryModification("org_brotli__dec", Libs.LibraryFields.AUTHOR_NAME, "Google")

            if (BuildConfig.FLAVOR != "gplay") {
                builder
                    .withAboutSpecial2(getString(R.string.app_info_donate))
                    .withAboutSpecial2Description(getString(R.string.donate_message))
            }

            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, builder.supportFragment())
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
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/icsx5app"))
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(Constants.TAG, "No browser installed")
        }
    }

}
