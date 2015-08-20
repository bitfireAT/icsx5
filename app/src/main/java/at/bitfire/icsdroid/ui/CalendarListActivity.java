package at.bitfire.icsdroid.ui;

import android.app.LoaderManager;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.ContentObserver;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

import at.bitfire.ical4android.CalendarStorageException;
import at.bitfire.icsdroid.AppAccount;
import at.bitfire.icsdroid.R;
import at.bitfire.icsdroid.db.LocalCalendar;

public class CalendarListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<LocalCalendar[]>, AdapterView.OnItemClickListener {

    ListView list;
    CalendarListAdapter listAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.title_activity_calendar_list);
        setContentView(R.layout.calendar_list_activity);

        list = (ListView)findViewById(R.id.calendar_list);
        list.setAdapter(listAdapter = new CalendarListAdapter(this));
        list.setOnItemClickListener(this);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_calendar_list, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean canSync = !listAdapter.isEmpty();
        menu.findItem(R.id.sync_all)
            .setEnabled(canSync)
            .setVisible(canSync);
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        LocalCalendar calendar = ((CalendarListAdapter) parent.getAdapter()).getItem(position);

        Intent i = new Intent(this, EditCalendarActivity.class);
        i.setData(ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendar.getId()));
        startActivity(i);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    /* loader callbacks */

    @Override
    public Loader<LocalCalendar[]> onCreateLoader(int id, Bundle args) {
        return new CalendarListLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<LocalCalendar[]> loader, LocalCalendar[] calendars) {
        listAdapter.clear();
        listAdapter.addAll(calendars);
        invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<LocalCalendar[]> loader) {
    }


    /* actions */

    public void onAddCalendar(View v) {
        startActivity(new Intent(this, AddCalendarActivity.class));
    }

    public void onShowInfo(MenuItem item) {
        Toast.makeText(this, "ICSdroid Info & Licenses!", Toast.LENGTH_SHORT).show();
    }

    public void onShowSettings(MenuItem item) {
        Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
    }

    public void onSyncAll(MenuItem item) {
        Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY, true);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_IGNORE_SETTINGS, true);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_IGNORE_BACKOFF, true);
        getContentResolver().requestSync(AppAccount.account, CalendarContract.AUTHORITY, extras);
        Snackbar.make(list, getString(R.string.calendar_list_sync_started), Snackbar.LENGTH_LONG).show();
    }


    public static class CalendarListAdapter extends ArrayAdapter<LocalCalendar> {
        private static final String TAG = "ICSdroid.CalendarList";

        final Context context;

        public CalendarListAdapter(Context context) {
            super(context, R.layout.calendar_list_item);
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null)
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.calendar_list_item, parent, false);

            LocalCalendar calendar = getItem(position);
            ((TextView) v.findViewById(R.id.url)).setText(calendar.getUrl());
            ((TextView) v.findViewById(R.id.title)).setText(calendar.getDisplayName());

            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT);
            ((TextView) v.findViewById(R.id.last_sync)).setText(calendar.getLastSync() == 0 ?
                    context.getString(R.string.calendar_list_not_synced_yet) :
                    formatter.format(new Date(calendar.getLastSync())));

            ((ColorButton) v.findViewById(R.id.color)).setColor(calendar.getColor().intValue());

            String errorMessage = calendar.getErrorMessage();
            TextView textError = (TextView) v.findViewById(R.id.error_message);
            if (errorMessage == null)
                textError.setVisibility(View.INVISIBLE);
            else {
                textError.setText(errorMessage);
                textError.setVisibility(View.VISIBLE);
            }

            /*v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    try {
                        LocalCalendar calendar = getI;
                        calendar.delete();
                        calendars.remove(calendar);
                        notifyDataSetChanged();
                    } catch (CalendarStorageException e) {
                        Toast.makeText(v.getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
            });*/
            return v;
        }
    }

    protected static class CalendarListLoader extends Loader<LocalCalendar[]> {
        private static final String TAG = "ICSdroid.CalendarsLoad";

        ContentProviderClient provider;
        ContentObserver observer;

        public CalendarListLoader(Context context) {
            super(context);
        }

        @Override
        protected void onStartLoading() {
            ContentResolver resolver = getContext().getContentResolver();
            provider = resolver.acquireContentProviderClient(CalendarContract.AUTHORITY);

            observer = new ForceLoadContentObserver();
            resolver.registerContentObserver(CalendarContract.Calendars.CONTENT_URI, false, observer);

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
                LocalCalendar[] calendars;
                if (provider != null && AppAccount.isAvailable(getContext()))
                    calendars = LocalCalendar.findAll(AppAccount.account, provider);
                else
                    calendars = LocalCalendar.Factory.FACTORY.newArray(0);
                deliverResult(calendars);
            } catch (CalendarStorageException e) {
                Log.e(TAG, "Couldn't load calendar list", e);
            }
        }

    }

}
