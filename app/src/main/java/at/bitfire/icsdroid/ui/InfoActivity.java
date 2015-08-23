package at.bitfire.icsdroid.ui;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

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
        tab.setContent(R.id.icsdroid_info);
        tabs.addTab(tab);

        addLibraryTab(tabs, "Android", "Android Support Library", "https://developer.android.com/tools/support-library/", "licenses/LICENSE.android-support");
        addLibraryTab(tabs, "Ambilwarna", "Android Color Picker", "https://github.com/yukuku/ambilwarna", "licenses/LICENSE.ambilwarna");
        addLibraryTab(tabs, "Commons", "Apache Commons", "https://commons.apache.org/", "licenses/LICENSE.commons");
        addLibraryTab(tabs, "bnd", "bnd, OSGi Core", "http://bnd.bndtools.org/", "licenses/LICENSE.bnd");
        addLibraryTab(tabs, "ical4j", "ical4j", "https://github.com/ical4j/ical4j/", "licenses/LICENSE.ical4j");
        addLibraryTab(tabs, "SLF4J", "SLF4J, SLF4J Android", "http://www.slf4j.org/", "licenses/LICENSE.slf4j");
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
                    @Cleanup InputStream is = getAssets().open(licenseFile);
                    ((TextView)v.findViewById(R.id.library_license)).setText(IOUtils.toString(is));
                } catch (IOException e) {
                    Log.e(TAG, "Couldn't read library license", e);
                }
                return v;
            }
        });
        tabs.addTab(tab);
    }

}
