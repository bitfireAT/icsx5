/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.text.util.Linkify
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import at.bitfire.icsdroid.BuildConfig
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.HttpClient
import at.bitfire.icsdroid.R
import kotlinx.android.synthetic.main.app_info_activity.*
import kotlinx.android.synthetic.main.app_info_component.view.*
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.concurrent.thread

class InfoActivity: AppCompatActivity() {

    companion object {
        val components = arrayOf(
                arrayOf("ICSx⁵", "ICSx⁵/${BuildConfig.VERSION_NAME}", "Ricki Hirner, Bernhard Stockmann (bitfire.at)", "https://icsx5.bitfire.at", "gpl-3.0-standalone.html"),
                arrayOf("Apache Commons", "Apache Commons", "Apache Software Foundation", "http://commons.apache.org/", "apache2.html"),
                arrayOf("ColorPicker", "Color Picker", "Jared Rummler", "https://github.com/jaredrummler/ColorPicker", "mit.html"),
                arrayOf("ical4j", "ical4j/${at.bitfire.ical4android.BuildConfig.version_ical4j}", "Ben Fortuna", "https://ical4j.github.io", "bsd-3clause.html")
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.app_info_activity)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewpager.adapter = TabsAdapter(supportFragmentManager)
        tabs.setupWithViewPager(viewpager)

        if (savedInstanceState == null)
            ServiceLoader.load(StartupFragment::class.java).forEach { factory ->
                factory.initialize(this)
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.app_info_activity, menu)
        return true
    }

    override fun onPause() {
        super.onPause()
        HttpClient.setForeground(false)
    }

    override fun onResume() {
        super.onResume()
        HttpClient.setForeground(true)
    }

    fun showWebSite(item: MenuItem) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://icsx5.bitfire.at/?pk_campaign=icsx5-app&pk_kwd=info-activity")))
    }

    fun showTwitter(item: MenuItem) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/icsx5app")))
    }


    class TabsAdapter(
            fm: FragmentManager
    ): FragmentPagerAdapter(fm) {

        override fun getCount() = components.size
        override fun getPageTitle(position: Int) = components[position][0]
        override fun getItem(position: Int) = ComponentFragment.instantiate(position)

    }

    class ComponentFragment: Fragment() {

        companion object {

            const val KEY_POSITION = "position"

            fun instantiate(position: Int): ComponentFragment {
                val frag = ComponentFragment()
                val args = Bundle(1)
                args.putInt(KEY_POSITION, position)
                frag.arguments = args
                return frag
            }

        }

        @SuppressLint("SetTextI18n")
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            val info = components[arguments!!.getInt(KEY_POSITION)]

            val v = inflater.inflate(R.layout.app_info_component, container, false)

            v.title.text = info[1]
            v.copyright.text = "© ${info[2]}"

            v.url.autoLinkMask = Linkify.WEB_URLS
            v.url.text = info[3]

            // load and format license text
            val model = ViewModelProviders.of(this).get(LicenseModel::class.java)
            model.getHtml(info[4]).observe(this, Observer { formattedText ->
                v.license.apply {
                    text = formattedText
                    autoLinkMask = Linkify.WEB_URLS
                }
            })

            return v
        }

    }


    class LicenseModel(
            application: Application
    ): AndroidViewModel(application) {

        fun getHtml(fileName: String) =
                LicenseData(getApplication(), "license/$fileName")

    }

    class LicenseData(
            context: Context,
            fileName: String
    ): LiveData<Spanned>() {

        init {
            thread {
                try {
                    context.resources.assets.open(fileName).use {
                        val html = IOUtils.toString(it, StandardCharsets.UTF_8)
                        postValue(Html.fromHtml(html))
                    }
                } catch(e: IOException) {
                    Log.e(Constants.TAG, "Couldn't load license text", e)
                }
            }
        }

    }

}
