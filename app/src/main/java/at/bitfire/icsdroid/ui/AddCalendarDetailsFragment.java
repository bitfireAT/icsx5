package at.bitfire.icsdroid.ui;

import android.accounts.AccountManager;
import android.content.ContentValues;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import android.widget.TextView;
import android.widget.Toast;

import at.bitfire.ical4android.AndroidCalendar;
import at.bitfire.ical4android.CalendarStorageException;
import at.bitfire.icsdroid.AppAccount;
import at.bitfire.icsdroid.R;
import yuku.ambilwarna.AmbilWarnaDialog;

public class AddCalendarDetailsFragment extends Fragment implements TextWatcher {
    private static final String TAG = "ICSdroid.CreateCalendar";

    private static final String STATE_COLOR = "color";

    AddCalendarActivity activity;

    TextView textURL;
    EditText editTitle;
    ColorButton colorButton;

    String title;
    int color = 0xffFF0000;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
        View v = inflater.inflate(R.layout.add_calendar_details, container, false);
        setHasOptionsMenu(true);

        textURL = (TextView)v.findViewById(R.id.url);

        editTitle = (EditText)v.findViewById(R.id.title);
        editTitle.addTextChangedListener(this);

        colorButton = (ColorButton)v.findViewById(R.id.color);
        if (inState != null)
            colorButton.setColor(color = inState.getInt(STATE_COLOR));
        colorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AmbilWarnaDialog(getActivity(), color, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onCancel(AmbilWarnaDialog ambilWarnaDialog) {
                    }

                    @Override
                    public void onOk(AmbilWarnaDialog ambilWarnaDialog, int newColor) {
                        colorButton.setColor(color = 0xff000000 | newColor);
                    }
                }).show();
            }
        });

        return v;
    }

    @Override
    public void onActivityCreated(Bundle inState) {
        super.onActivityCreated(inState);
        activity = (AddCalendarActivity)getActivity();

        textURL.setText(activity.url.toString());

        if (inState == null) {
            String path = activity.url.getPath();
            editTitle.setText(path.substring(path.lastIndexOf('/') + 1));
            editTitle.requestFocus();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_COLOR, color);
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


    /* actions */

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
            activity.invalidateOptionsMenu();
            return true;
        } catch (CalendarStorageException e) {
            Log.e(TAG, "Couldn't create calendar", e);
            Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

}
