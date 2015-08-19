package at.bitfire.icsdroid.ui;

import android.app.LoaderManager;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.ContentObserver;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import at.bitfire.ical4android.CalendarStorageException;
import at.bitfire.icsdroid.AppAccount;
import at.bitfire.icsdroid.R;
import at.bitfire.icsdroid.db.LocalCalendar;

public class CalendarListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<LocalCalendar[]> {

    ListView list;
    CalendarListAdapter listAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.title_activity_calendar_list);
        setContentView(R.layout.activity_calendar_list);


        list = (ListView)findViewById(R.id.calendar_list);
        list.setAdapter(listAdapter = new CalendarListAdapter(this));

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_calendar_list, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    /* loader callbacks */

    @Override
    public Loader<LocalCalendar[]> onCreateLoader(int id, Bundle args) {
        Loader loader = new CalendarListLoader(this, listAdapter);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<LocalCalendar[]> loader, LocalCalendar[] calendars) {
        listAdapter.clear();
        listAdapter.addAll(calendars);
    }

    @Override
    public void onLoaderReset(Loader<LocalCalendar[]> loader) {
    }


    /* actions */

    public void onAddCalendar(View v) {
        startActivity(new Intent(this, AddAccountActivity.class));
    }

    public void onShowInfo(MenuItem item) {
        Toast.makeText(this, "ICSdroid Info & Licenses!", Toast.LENGTH_SHORT).show();
    }

    public void onShowSettings(MenuItem item) {
        Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
    }


    public static class CalendarListAdapter extends ArrayAdapter<LocalCalendar> {
        private static final String TAG = "ICSdroid.CalendarList";

        public CalendarListAdapter(Context context) {
            super(context, R.layout.calendar_list_item);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null)
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.calendar_list_item, parent, false);

            LocalCalendar calendar = getItem(position);
            ((TextView) v.findViewById(R.id.url)).setText(calendar.getUrl());
            ((TextView) v.findViewById(R.id.title)).setText(calendar.getDisplayName());
            ((ColorButton) v.findViewById(R.id.color)).setColor(calendar.getColor().intValue());

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

    public static class CalendarListLoader extends Loader<LocalCalendar[]> {
        private static final String TAG = "ICSdroid.CalendarsLoad";

        final Context context;

        ContentProviderClient provider;
        ContentObserver observer;

        public CalendarListLoader(Context context, CalendarListAdapter adapter) {
            super(context);
            this.context = context;
        }

        @Override
        protected void onStartLoading() {
            ContentResolver resolver = context.getContentResolver();
            provider = resolver.acquireContentProviderClient(CalendarContract.AUTHORITY);

            observer = new ForceLoadContentObserver();
            resolver.registerContentObserver(CalendarContract.Calendars.CONTENT_URI, false, observer);

            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            context.getContentResolver().unregisterContentObserver(observer);
            if (provider != null)
                provider.release();
        }

        @Override
        public void onForceLoad() {
            try {
                LocalCalendar[] calendars = LocalCalendar.findAll(AppAccount.account, provider);
                deliverResult(calendars);
            } catch (CalendarStorageException e) {
                Log.e(TAG, "Couldn't load calendar list", e);
            }
        }

    }

}
