package at.bitfire.icsdroid.ui;

import android.accounts.AccountManager;
import android.app.Fragment;
import android.content.ContentValues;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.apache.commons.lang3.StringUtils;
import android.provider.CalendarContract.Calendars;
import android.widget.Toast;

import at.bitfire.ical4android.AndroidCalendar;
import at.bitfire.ical4android.CalendarStorageException;
import at.bitfire.icsdroid.AppAccount;
import at.bitfire.icsdroid.R;

public class CalendarDetailsFragment extends Fragment implements TextWatcher {
    private static final String TAG = "ICSdroid.CreateCalendar";
    AddAccountActivity activity;

    EditText editTitle;

    String title;
    long color = 0xFFFF0000;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = (AddAccountActivity)getActivity();

        View v = inflater.inflate(R.layout.fragment_calendar_details, container, false);
        setHasOptionsMenu(true);

        editTitle = (EditText)v.findViewById(R.id.title);
        editTitle.addTextChangedListener(this);
        if (savedInstanceState == null) {
            String path = activity.url.getPath();
            editTitle.setText(path.substring(path.lastIndexOf('/') + 1));
        }
        editTitle.requestFocus();

        return v;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem itemGo = menu.findItem(R.id.create_calendar);
        itemGo.setEnabled(StringUtils.isNotBlank(title));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_create_calendar, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.create_calendar) {
            if (createCalendar())
                activity.finish();
            return true;
        }
        return false;
    }

    private boolean createCalendar() {
        AccountManager am = AccountManager.get(activity);
        am.addAccountExplicitly(AppAccount.account, null, null);

        ContentValues calInfo = new ContentValues();
        calInfo.put(Calendars.ACCOUNT_NAME, AppAccount.account.name);
        calInfo.put(Calendars.ACCOUNT_TYPE, AppAccount.account.type);
        calInfo.put(Calendars.NAME, activity.url.toString());
        calInfo.put(Calendars.CALENDAR_DISPLAY_NAME, title);
        calInfo.put(Calendars.CALENDAR_COLOR, color);
        calInfo.put(Calendars.OWNER_ACCOUNT, AppAccount.account.name);
        calInfo.put(Calendars.SYNC_EVENTS, 1);
        calInfo.put(Calendars.VISIBLE, 1);
        calInfo.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_READ);
        try {
            AndroidCalendar.create(AppAccount.account, activity.getContentResolver(), calInfo);
            Toast.makeText(activity, getString(R.string.add_account_calendar_created), Toast.LENGTH_LONG).show();
            return true;
        } catch (CalendarStorageException e) {
            Log.e(TAG, "Couldn't create calendar", e);
            Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }


    /* dynamic changes */

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        title = editTitle.getText().toString();
        activity.invalidateOptionsMenu();
    }
}
