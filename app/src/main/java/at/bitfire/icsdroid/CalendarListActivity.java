package at.bitfire.icsdroid;

import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import at.bitfire.ical4android.CalendarStorageException;

public class CalendarListActivity extends AppCompatActivity {
    RecyclerView calendarList;
    ContentProviderClient provider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.title_activity_calendar_list);
        setContentView(R.layout.activity_calendar_list);

        provider = getContentResolver().acquireContentProviderClient(CalendarContract.AUTHORITY);

        calendarList = (RecyclerView)findViewById(R.id.calendar_list);
        calendarList.setLayoutManager(new LinearLayoutManager(this));
        calendarList.setAdapter(new CalendarListAdapter(provider));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_calendar_list, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (provider != null)
            provider.release();
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


    public static class CalendarListAdapter extends RecyclerView.Adapter<CalendarListAdapter.ViewHolder> {
        private static final String TAG = "ICSdroid.CalendarList";
        private List<LocalCalendar> calendars = new LinkedList<>();

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textURL;
            public ViewHolder(View view) {
                super(view);
                this.textURL = (TextView)view.findViewById(R.id.url);
            }
        }

        public CalendarListAdapter(ContentProviderClient provider) {
            try {
                calendars.addAll(Arrays.asList(LocalCalendar.findAll(AppAccount.account, provider)));
            } catch (CalendarStorageException e) {
                Log.e(TAG, "Couldn't enumerate calendars", e);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(final ViewGroup parent, final int i) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.calendar_list_item, parent, false);
            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    try {
                        LocalCalendar calendar = calendars.get(i);
                        calendar.delete();
                        calendars.remove(calendar);
                        notifyDataSetChanged();
                    } catch (CalendarStorageException e) {
                        Toast.makeText(v.getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
            });
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int i) {
            viewHolder.textURL.setText(calendars.get(i).url);
        }

        @Override
        public int getItemCount() {
            return calendars.size();
        }

    }
}
