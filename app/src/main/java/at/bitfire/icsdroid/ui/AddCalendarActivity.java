package at.bitfire.icsdroid.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.net.URL;

import at.bitfire.icsdroid.R;

public class AddCalendarActivity extends AppCompatActivity {
    private static final String TAG = "ICSdroid.AddCalendar";
    private static final String
            STATE_URL = "url",
            STATE_AUTH_REQUIRED = "auth_required",
            STATE_USERNAME = "username",
            STATE_PASSWORD = "password";

    URL url;
    boolean authRequired = false;
    String username, password;

    @Override
    protected void onCreate(Bundle inState) {
        super.onCreate(inState);
        setContentView(R.layout.add_calendar_activity);

        if (inState == null)
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, new AddCalendarEnterUrlFragment())
                    .commit();
        else {
            url = (URL)inState.getSerializable(STATE_URL);
            authRequired = inState.getBoolean(STATE_AUTH_REQUIRED);
            username = inState.getString(STATE_USERNAME);
            password = inState.getString(STATE_PASSWORD);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_URL, url);
        outState.putBoolean(STATE_AUTH_REQUIRED, authRequired);
        outState.putString(STATE_USERNAME, username);
        outState.putString(STATE_PASSWORD, password);
    }
}
