/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import at.bitfire.icsdroid.BuildConfig
import at.bitfire.icsdroid.R
import com.mikepenz.aboutlibraries.LibsBuilder

class InfoActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            val builder = LibsBuilder().apply {
                aboutShowIcon = true
                aboutAppName = getString(R.string.app_name)
                aboutAppSpecial1 = getString(R.string.app_info_gplv3)
                aboutAppSpecial1Description = getString(R.string.app_info_gplv3_note)
                if (BuildConfig.FLAVOR != "gplay") {
                    aboutAppSpecial2 = getString(R.string.app_info_donate)
                    aboutAppSpecial2Description = getString(R.string.donate_message)
                }
                aboutDescription = getString(R.string.app_info_description)
                aboutVersionString = getString(R.string.app_info_version, BuildConfig.VERSION_NAME, BuildConfig.FLAVOR)
                showLicense = true
            }
            builder.supportFragment()

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
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://icsx5.bitfire.at/?pk_campaign=icsx5-app&pk_kwd=info-activity")))
    }

    fun showTwitter(item: MenuItem) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/icsx5app")))
    }

}
