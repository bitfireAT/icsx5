package at.bitfire.icsdroid.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;

import at.bitfire.ical4android.CalendarStorageException;
import at.bitfire.icsdroid.AppAccount;
import at.bitfire.icsdroid.R;
import at.bitfire.icsdroid.db.LocalCalendar;

public class EditCalendarActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<LocalCalendar> {
    private static final String
            STATE_TITLE = "title",
            STATE_COLOR = "color",
            STATE_SYNC_THIS = "sync_this",
            STATE_REQUIRE_AUTH = "requires_auth",
            STATE_USERNAME = "username",
            STATE_PASSWORD = "password",
            STATE_DIRTY = "dirty";

    Bundle savedState;
    LocalCalendar calendar;
    boolean dirty;           // indicates whether title/color have been changed by the user

    TitleColorFragment fragTitleColor;
    Switch switchSyncCalendar;
    CredentialsFragment fragCredentials;


    @Override
    protected void onCreate(Bundle inState) {
        super.onCreate(inState);
        setContentView(R.layout.edit_calendar);

        switchSyncCalendar = (Switch)findViewById(R.id.sync_calendar);
        switchSyncCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDirty(true);
            }
        });

        // load calendar from provider
        getLoaderManager().initLoader(0, null, this);

        savedState = inState;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (fragTitleColor != null) {
            outState.putString(STATE_TITLE, fragTitleColor.title);
            outState.putInt(STATE_COLOR, fragTitleColor.color);
        }
        outState.putBoolean(STATE_SYNC_THIS, switchSyncCalendar.isChecked());
        if (fragCredentials != null) {
            outState.putBoolean(STATE_REQUIRE_AUTH, fragCredentials.authRequired);
            outState.putString(STATE_USERNAME, fragCredentials.username);
            outState.putString(STATE_PASSWORD, fragCredentials.password);
        }
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

        boolean titleOK = StringUtils.isNotBlank(fragTitleColor.title),
                authOK = !fragCredentials.authRequired || (StringUtils.isNotBlank(fragCredentials.username) && StringUtils.isNotBlank(fragCredentials.password) );
        menu.findItem(R.id.save)
                .setEnabled(dirty && titleOK && authOK)
                .setVisible(dirty && titleOK && authOK);
        return true;
    }

    protected void setDirty(boolean dirty) {
        this.dirty = dirty;
        invalidateOptionsMenu();
    }


    /* user actions */

    @Override
    public void onBackPressed() {
        if (dirty) {
            Fragment fragment = SaveDismissDialogFragment.newInstance();
            getFragmentManager().beginTransaction()
                    .add(fragment, null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        } else
            super.onBackPressed();
    }

    public void onSave(MenuItem item) {
        boolean success = false;
        try {
            if (calendar != null) {
                ContentValues values = new ContentValues(2);
                values.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, fragTitleColor.title);
                values.put(CalendarContract.Calendars.CALENDAR_COLOR, fragTitleColor.color);
                values.put(CalendarContract.Calendars.SYNC_EVENTS, switchSyncCalendar.isChecked() ? 1 : 0);
                calendar.update(values);
                success = true;
            }
        } catch (CalendarStorageException e) {
        }
        Toast.makeText(this, getString(success ? R.string.edit_calendar_saved : R.string.edit_calendar_failed), Toast.LENGTH_SHORT).show();
        finish();
    }

    public void onAskDelete(MenuItem item) {
        getFragmentManager().beginTransaction()
                .add(DeleteDialogFragment.newInstance(), null)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    protected void onDelete() {
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

            if (fragTitleColor == null) {
                fragTitleColor = new TitleColorFragment();
                Bundle args = new Bundle(3);
                args.putString(TitleColorFragment.ARG_URL, calendar.getName());
                args.putString(TitleColorFragment.ARG_TITLE, savedState == null ? calendar.getDisplayName() : savedState.getString(STATE_TITLE));
                args.putInt(TitleColorFragment.ARG_COLOR, savedState == null ? calendar.getColor() : savedState.getInt(STATE_COLOR));
                fragTitleColor.setArguments(args);
                fragTitleColor.setOnChangeListener(new TitleColorFragment.OnChangeListener() {
                    @Override
                    public void onChangeTitleColor(String title, int color) {
                        setDirty(true);
                    }
                });
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.title_color, fragTitleColor)
                        .commit();
            }

            if (fragCredentials == null) {
                fragCredentials = new CredentialsFragment();
                Bundle args = new Bundle(3);
                boolean authRequired = savedState == null ? (calendar.getUsername() != null && calendar.getPassword() != null) : savedState.getBoolean(STATE_REQUIRE_AUTH);
                args.putBoolean(CredentialsFragment.ARG_AUTH_REQUIRED, authRequired);
                args.putString(CredentialsFragment.ARG_USERNAME, savedState == null ? calendar.getUsername() : savedState.getString(STATE_USERNAME));
                args.putString(CredentialsFragment.ARG_PASSWORD, savedState == null ? calendar.getPassword() : savedState.getString(STATE_PASSWORD));
                fragCredentials.setArguments(args);
                fragCredentials.setOnChangeListener(new CredentialsFragment.OnCredentialsChangeListener() {
                    @Override
                    public void onChangeCredentials(boolean authRequired, String username, String password) {
                        setDirty(true);
                    }
                });
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.credentials, fragCredentials)
                        .commit();
            }

            switchSyncCalendar.setChecked(savedState == null ? calendar.isSynced() : savedState.getBoolean(STATE_SYNC_THIS));

            setDirty(savedState == null ? false : savedState.getBoolean(STATE_DIRTY));
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
        boolean loaded;

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

            synchronized(uri) {
                if (!loaded) {
                    forceLoad();
                    loaded = true;
                }
            }
        }

        @Override
        protected void onStopLoading() {
            getContext().getContentResolver().unregisterContentObserver(observer);
            if (provider != null)
                provider.release();
        }

        @Override
        public void onForceLoad() {
            LocalCalendar calendar = null;
            if (provider != null)
                try {
                    calendar = LocalCalendar.findById(AppAccount.account, provider, ContentUris.parseId(uri));
                } catch (FileNotFoundException e) {
                    // calendar has been removed in the meanwhile, return null
                } catch (CalendarStorageException e) {
                    Log.e(TAG, "Couldn't load calendar data", e);
                }
            deliverResult(calendar);
        }
    }


    /* "Save or dismiss" dialog */

    public static class SaveDismissDialogFragment extends DialogFragment {
        public static SaveDismissDialogFragment newInstance() {
            return new SaveDismissDialogFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.edit_calendar_unsaved_changes)
                    .setPositiveButton(R.string.edit_calendar_save, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            ((EditCalendarActivity) getActivity()).onSave(null);
                        }
                    })
                    .setNegativeButton(R.string.edit_calendar_dismiss, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            ((EditCalendarActivity) getActivity()).onCancel(null);
                        }
                    })
                    .create();
        }
    }


    /* "Really delete?" dialog */

    public static class DeleteDialogFragment extends DialogFragment {
        public static DeleteDialogFragment newInstance() {
            return new DeleteDialogFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.edit_calendar_really_delete)
                    .setPositiveButton(R.string.edit_calendar_delete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            ((EditCalendarActivity) getActivity()).onDelete();
                        }
                    })
                    .setNegativeButton(R.string.edit_calendar_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create();
        }
    }

}
