/*
 * Copyright (c) 2013 – 2015 Ricki Hirner (bitfire web engineering).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 */

package at.bitfire.icsdroid.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import at.bitfire.icsdroid.BuildConfig;
import at.bitfire.icsdroid.Constants;
import at.bitfire.icsdroid.R;
import lombok.Cleanup;

public class InfoActivity extends AppCompatActivity {
    private static final String TAG = "ICSdroid.info";

    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_info);

        final TabHost tabs = (TabHost)findViewById(android.R.id.tabhost);
        tabs.setup();

        TabHost.TabSpec tab = tabs.newTabSpec("ICSdroid");
        if (Build.VERSION.SDK_INT >= 20)
            tab.setIndicator("ICSdroid", getDrawable(R.drawable.ic_launcher));
        else
            tab.setIndicator("ICSdroid");
        tab.setContent(new AppInfoTabFactory(tabs));
        tabs.addTab(tab);

        addLibraryTab(tabs, "Android", "Android Support Library", "https://developer.android.com/tools/support-library/", "LICENSE.android-support");
        addLibraryTab(tabs, "Ambilwarna", "Android Color Picker", "https://github.com/yukuku/ambilwarna", "LICENSE.ambilwarna");
        addLibraryTab(tabs, "Commons", "Apache Commons", "https://commons.apache.org/", "LICENSE.commons");
        addLibraryTab(tabs, "bnd", "bnd, OSGi Core", "http://bnd.bndtools.org/", "LICENSE.bnd");
        addLibraryTab(tabs, "ical4j", "ical4j", "https://github.com/ical4j/ical4j/", "LICENSE.ical4j");
        addLibraryTab(tabs, "SLF4J", "SLF4J, SLF4J Android", "http://www.slf4j.org/", "LICENSE.slf4j");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_info_activity, menu);
        return true;
    }

    protected void addLibraryTab(final TabHost tabs, final String tag, final String title, final String url, final String licenseFile) {
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


    public void showWebSite(MenuItem item) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://icsdroid.bitfire.at/?pk_campaign=icsdroid-app&pk_kwd=info-activity")));
    }

    public void showDonate(MenuItem item) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://icsdroid.bitfire.at/donate?pk_campaign=icsdroid-app&pk_kwd=info-activity")));
    }



    class AppInfoTabFactory implements TabHost.TabContentFactory {

        final TabHost tabs;

        AppInfoTabFactory(TabHost tabs) {
            this.tabs = tabs;
        }

        @Override
        public View createTabContent(String tag) {
            View v = getLayoutInflater().inflate(R.layout.app_info_icsdroid, tabs.getTabWidget(), false);

            ((TextView)v.findViewById(R.id.icsdroid_version)).setText("ICSdroid/" + BuildConfig.VERSION_NAME);

            try {
                @Cleanup InputStream is = getAssets().open("licenses/COPYING");
                ((TextView) v.findViewById(R.id.gpl_text)).setText(IOUtils.toString(is));
            } catch (IOException e) {
                Log.e(TAG, "Couldn't read GPLv3", e);
            }

            return v;
        }
    }

}
