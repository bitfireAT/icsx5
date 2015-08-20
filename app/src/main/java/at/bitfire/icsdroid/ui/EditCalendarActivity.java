package at.bitfire.icsdroid.ui;

import android.app.LoaderManager;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Loader;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;

import at.bitfire.ical4android.CalendarStorageException;
import at.bitfire.icsdroid.AppAccount;
import at.bitfire.icsdroid.R;
import at.bitfire.icsdroid.db.LocalCalendar;
import yuku.ambilwarna.AmbilWarnaDialog;

public class EditCalendarActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<LocalCalendar>, TextWatcher {
    private static final String
            STATE_COLOR = "color",
            STATE_DIRTY = "dirty";

    TextView textURL;
    EditText editTitle;
    ColorButton colorButton;

    String title;
    int color = 0xffFF0000;

    LocalCalendar calendar;
    boolean dirty;           // indicates whether title/color have been changed by the user

    @Override
    protected void onCreate(Bundle inState) {
        super.onCreate(inState);
        setContentView(R.layout.add_calendar_details);

        textURL = (TextView)findViewById(R.id.url);

        editTitle = (EditText)findViewById(R.id.title);
        editTitle.addTextChangedListener(this);

        colorButton = (ColorButton)findViewById(R.id.color);
        colorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AmbilWarnaDialog(EditCalendarActivity.this, color, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onCancel(AmbilWarnaDialog ambilWarnaDialog) {
                    }

                    @Override
                    public void onOk(AmbilWarnaDialog ambilWarnaDialog, int newColor) {
                        colorButton.setColor(color = 0xff000000 | newColor);
                        setDirty();
                    }
                }).show();
            }
        });

        if (inState != null) {
            dirty = inState.getBoolean(STATE_DIRTY);
            colorButton.setColor(color = inState.getInt(STATE_COLOR));
        }

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_COLOR, color);
        outState.putBoolean(STATE_DIRTY, dirty);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_edit_calendar, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.delete)
                .setEnabled(!dirty)
                .setVisible(!dirty);

        menu.findItem(R.id.cancel)
                .setEnabled(dirty)
                .setVisible(dirty);
        menu.findItem(R.id.save)
                .setEnabled(dirty)
                .setVisible(dirty);
        return true;
    }

    protected void setDirty() {
        dirty = true;
        invalidateOptionsMenu();
    }


    /* user actions */

    @Override
    public void onBackPressed() {
        if (dirty) {
            Toast.makeText(this, "Speichern oder was?!", Toast.LENGTH_SHORT).show();
        } else
            super.onBackPressed();
    }

    public void onSave(MenuItem item) {
        boolean success = false;
        try {
            if (calendar != null) {
                ContentValues values = new ContentValues(2);
                values.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, title);
                values.put(CalendarContract.Calendars.CALENDAR_COLOR, color);
                calendar.update(values);
                success = true;
            }
        } catch (CalendarStorageException e) {
        }
        Toast.makeText(this, getString(success ? R.string.edit_calendar_saved : R.string.edit_calendar_failed), Toast.LENGTH_SHORT).show();
        finish();
    }

    public void onDelete(MenuItem item) {
        boolean success = false;
        try {
            if (calendar != null) {
                calendar.delete();
                success = true;
            }
        } catch (CalendarStorageException e) {
        }
        Toast.makeText(this, getString(success ? R.string.edit_calendar_deleted : R.string.edit_calendar_failed), Toast.LENGTH_SHORT).show();
        finish();
    }

    public void onCancel(MenuItem item) {
        finish();
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
        setDirty();
    }


    /* loader callbacks */

    @Override
    public Loader<LocalCalendar> onCreateLoader(int id, Bundle args) {
        return new CalendarLoader(this, getIntent().getData());
    }

    @Override
    public void onLoadFinished(Loader<LocalCalendar> loader, LocalCalendar calendar) {
        if (calendar == null)
            // calendar not available (anymore), close activity
            finish();
        else {
            this.calendar = calendar;
            textURL.setText(calendar.getUrl());
            editTitle.setText(title = calendar.getDisplayName());
            colorButton.setColor(color = calendar.getColor());
            dirty = false;
            invalidateOptionsMenu();
        }
    }

    @Override
    public void onLoaderReset(Loader<LocalCalendar> loader) {
        calendar = null;
    }


    /* loader */

    protected static class CalendarLoader extends Loader<LocalCalendar> {
        private static final String TAG = "ICSdroid.Calendar";

        final Uri uri;
        ContentProviderClient provider;
        ContentObserver observer;

        public CalendarLoader(Context context, Uri uri) {
            super(context);
            this.uri = uri;
        }

        @Override
        protected void onStartLoading() {
            ContentResolver resolver = getContext().getContentResolver();
            provider = resolver.acquireContentProviderClient(CalendarContract.AUTHORITY);

            observer = new ForceLoadContentObserver();
            resolver.registerContentObserver(uri, false, observer);

            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            getContext().getContentResolver().unregisterContentObserver(observer);
            if (provider != null)
                provider.release();
        }

        @Override
        public void onForceLoad() {
            try {
                LocalCalendar calendar = null;
                if (provider != null)
                    try {
                        calendar = LocalCalendar.findById(AppAccount.account, provider);
                    } catch (FileNotFoundException e) {
                        // calendar has been removed in the meanwhile, return null
                    }
                deliverResult(calendar);
            } catch (CalendarStorageException e) {
                Log.e(TAG, "Couldn't load calendar list", e);
            }
        }
    }
}
