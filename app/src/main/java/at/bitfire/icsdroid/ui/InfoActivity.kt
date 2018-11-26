/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui

import android.annotation.SuppressLint
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
import androidx.loader.app.LoaderManager
import androidx.loader.content.AsyncTaskLoader
import androidx.loader.content.Loader
import at.bitfire.icsdroid.BuildConfig
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R
import kotlinx.android.synthetic.main.app_info_activity.*
import kotlinx.android.synthetic.main.app_info_component.view.*
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.nio.charset.StandardCharsets

class InfoActivity: AppCompatActivity() {

    companion object {
        val components = arrayOf(
                arrayOf("ICSdroid", "ICSdroid/${BuildConfig.VERSION_NAME}", "Ricki Hirner, Bernhard Stockmann (bitfire.at)", "https://icsdroid.bitfire.at", "gpl-3.0-standalone.html"),
                arrayOf("AmbilWarna", "AmbilWarna (Android Color Picker)", "Yuku", "https://github.com/yukuku/ambilwarna", "apache2.html"),
                arrayOf("Apache Commons", "Apache Commons", "Apache Software Foundation", "http://commons.apache.org/", "apache2.html"),
                arrayOf("ical4j", "ical4j", "Ben Fortuna", "https://ical4j.github.io", "bsd-3clause.html")
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.app_info_activity)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewpager.adapter = TabsAdapter(supportFragmentManager)
        tabs.setupWithViewPager(viewpager)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.app_info_activity, menu)
        return true
    }

    fun showWebSite(item: MenuItem) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://icsdroid.bitfire.at/?pk_campaign=icsdroid-app&pk_kwd=info-activity")))
    }

    fun showTwitter(item: MenuItem) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/icsdroidapp")))
    }


    class TabsAdapter(
            fm: FragmentManager
    ): FragmentPagerAdapter(fm) {

        override fun getCount() = components.size
        override fun getPageTitle(position: Int) = components[position][0]
        override fun getItem(position: Int) = ComponentFragment.instantiate(position)

    }

    class ComponentFragment: Fragment(), LoaderManager.LoaderCallbacks<Spanned> {

        companion object {

            const val KEY_POSITION = "position"
            const val KEY_LICENSE_FILE = "license_file"

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
            val args = Bundle(1)
            args.putString(KEY_LICENSE_FILE, "license/${info[4]}")
            LoaderManager.getInstance(this).initLoader(0, args, this)

            return v
        }

        override fun onCreateLoader(id: Int, args: Bundle?) =
                LicenseLoader(requireActivity(), args!!.getString(KEY_LICENSE_FILE)!!)

        override fun onLoadFinished(loader: Loader<Spanned?>, text: Spanned?) {
            text?.let {
                view?.license?.let {
                    it.autoLinkMask = Linkify.WEB_URLS
                    it.text = text
                }
            }
        }

        override fun onLoaderReset(loader: Loader<Spanned>) {
        }

    }

    class LicenseLoader(
            context: Context,
            private val fileName: String
    ): AsyncTaskLoader<Spanned>(context) {

        var text: Spanned? = null

        override fun onStartLoading() {
            Log.v(Constants.TAG, "Loading license text from $fileName")
            if (text == null)
                forceLoad()
            else
                deliverResult(text)
        }

        override fun loadInBackground(): Spanned? {
            try {
                context.resources.assets.open(fileName).use {
                    val html = IOUtils.toString(it, StandardCharsets.UTF_8)
                    text = Html.fromHtml(html)
                    return text
                }
            } catch(e: IOException) {
                Log.e(Constants.TAG, "Couldn't load license text", e)
            }
            return null
        }

    }

}
