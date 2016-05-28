/*
 * Copyright (c) 2013 – 2016 Ricki Hirner (bitfire web engineering).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 *
 */

package at.bitfire.icsdroid.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import at.bitfire.icsdroid.Constants;
import at.bitfire.icsdroid.R;
import lombok.Cleanup;

public class InfoActivity extends AppCompatActivity {
    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_info_activity);

        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));

        ViewPager pager = (ViewPager)findViewById(R.id.viewpager);
        pager.setAdapter(new TabsAdapter(getSupportFragmentManager()));

        TabLayout tabLayout = (TabLayout)findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(pager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_info_activity, menu);
        return true;
    }

    public void showWebSite(MenuItem item) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://icsdroid.bitfire.at/?pk_campaign=icsdroid-app&pk_kwd=info-activity")));
    }

    public void showTwitter(MenuItem item) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/icsdroidapp")));
    }

    public void showDonate(MenuItem item) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://icsdroid.bitfire.at/donate/?pk_campaign=icsdroid-app&pk_kwd=info-activity")));
    }


    static final String[][] components = {
            { "ICSdroid", "ICSdroid", "Ricki Hirner, Bernhard Stockmann (bitfire web engineering)", "https://icsdroid.bitfire.at", "gpl-3.0-standalone.html" },
            { "AmbilWarna", "AmbilWarna (Android Color Picker)", "Yuku", "https://github.com/yukuku/ambilwarna", "apache2.html" },
            { "Apache Commons", "Apache Commons", "Apache Software Foundation", "http://commons.apache.org/", "apache2.html" },
            { "ical4j", "ical4j", "Ben Fortuna", "https://ical4j.github.io", "bsd-3clause.html" },
            { "MTM", "MemorizingTrustManager", "Georg Lukas", "https://github.com/ge0rg/MemorizingTrustManager/", "mit.html" },
            { "Lombok", "Project Lombok", "The Project Lombok Authors", "https://projectlombok.org", "mit.html" }
    };

    static class TabsAdapter extends FragmentPagerAdapter {

        public TabsAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return components.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return components[position][0];
        }

        @Override
        public Fragment getItem(int position) {
            return ComponentFragment.instantiate(position);
        }

    }

    public static class ComponentFragment extends Fragment implements LoaderManager.LoaderCallbacks<Spanned> {
        private static final String
                KEY_POSITION = "position",
                KEY_LICENSE_FILE = "license_file";

        public static ComponentFragment instantiate(int position) {
            ComponentFragment frag = new ComponentFragment();
            Bundle args = new Bundle(1);
            args.putInt(KEY_POSITION, position);
            frag.setArguments(args);
            return frag;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            String[] info = components[getArguments().getInt(KEY_POSITION)];

            View v = inflater.inflate(R.layout.app_info_component, container, false);

            TextView tv = (TextView)v.findViewById(R.id.title);
            tv.setText(info[1]);

            tv = (TextView)v.findViewById(R.id.copyright);
            tv.setText("© " + info[2]);

            tv = (TextView)v.findViewById(R.id.url);
            tv.setAutoLinkMask(Linkify.WEB_URLS);
            tv.setText(info[3]);

            // load and format license text
            Bundle args = new Bundle(1);
            args.putString(KEY_LICENSE_FILE, "license/" + info[4]);
            getLoaderManager().initLoader(0, args, this);

            return v;
        }

        @Override
        public Loader<Spanned> onCreateLoader(int id, Bundle args) {
            return new LicenseLoader(getContext(), args.getString(KEY_LICENSE_FILE));
        }

        @Override
        public void onLoadFinished(Loader<Spanned> loader, Spanned text) {
            if (getView() != null) {
                TextView tv = (TextView)getView().findViewById(R.id.license);
                tv.setAutoLinkMask(Linkify.WEB_URLS);
                tv.setText(text);
            }
        }

        @Override
        public void onLoaderReset(Loader<Spanned> loader) {
        }
    }

    private static class LicenseLoader extends AsyncTaskLoader<Spanned> {

        final String fileName;
        Spanned text;

        public LicenseLoader(Context context, String fileName) {
            super(context);
            this.fileName = fileName;
        }

        @Override
        protected void onStartLoading() {
            Log.v(Constants.TAG, "Loading license text from " + fileName);
            if (text == null)
                forceLoad();
            else
                deliverResult(text);
        }

        @Override
        public Spanned loadInBackground() {
            try {
                @Cleanup InputStream is = getContext().getResources().getAssets().open(fileName);
                String html = IOUtils.toString(is, Charset.defaultCharset() /* UTF-8 */);
                return text = Html.fromHtml(html);
            } catch(IOException e) {
                Log.e(Constants.TAG, "Couldn't load license text", e);
                return null;
            }
        }

    }


    /*protected void addLibraryTab(final TabHost tabs, final String tag, final String title, final String url, final String licenseFile) {
        TabHost.TabSpec tab = tabs.newTabSpec(tag);
        tab.setIndicator(tag);
        tab.setContent(new TabHost.TabContentFactory() {
            @Override
            public View createTabContent(String tag) {
                View v = getLayoutInflater().inflate(R.layout.app_info_library, tabs.getTabWidget(), false);
                ((TextView)v.findViewById(R.id.library_name)).setText(title);
                ((TextView)v.findViewById(R.id.library_url)).setText(url);

                try {
                    @Cleanup InputStream is = getAssets().open("licenses/" + licenseFile);
                    ((TextView)v.findViewById(R.id.library_license)).setText(IOUtils.toString(is));
                } catch (IOException e) {
                    Log.e(TAG, "Couldn't read library license", e);
                }
                return v;
            }
        });
        tabs.addTab(tab);
    }



    class AppInfoTabFactory implements TabHost.TabContentFactory {

        final TabHost tabs;

        AppInfoTabFactory(TabHost tabs) {
            this.tabs = tabs;
        }

        @Override
        public View createTabContent(String tag) {
            View v = getLayoutInflater().inflate(R.layout.app_info_icsdroid, tabs.getTabWidget(), false);

            ((TextView)v.findViewById(R.id.icsdroid_version)).setText(getString(R.string.app_name) + "/" + BuildConfig.VERSION_NAME);

            try {
                @Cleanup InputStream is = getAssets().open("licenses/COPYING");
                ((TextView) v.findViewById(R.id.gpl_text)).setText(IOUtils.toString(is));
            } catch (IOException e) {
                Log.e(TAG, "Couldn't read GPLv3", e);
            }

            return v;
        }
    }*/

}
