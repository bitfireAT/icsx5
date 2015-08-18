package at.bitfire.icsdroid;

import android.app.Fragment;
import android.net.Uri;
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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;

public class EnterUrlFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, TextWatcher {
    private static final String TAG = "ICSdroid.EnterUrl";

    AddAccountActivity activity;

    EditText editURL, editUsername, editPassword;
    TextView insecureAuthWarning, textUsername, textPassword;
    Switch switchAuthRequired;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = (AddAccountActivity)getActivity();

        View v = inflater.inflate(R.layout.fragment_enter_url, container, false);
        setHasOptionsMenu(true);

        editURL = (EditText)v.findViewById(R.id.url);
        switchAuthRequired = (Switch)v.findViewById(R.id.requires_authentication);
        insecureAuthWarning = (TextView)v.findViewById(R.id.insecure_authentication_warning);
        textUsername = (TextView)v.findViewById(R.id.user_name_label);
        editUsername = (EditText)v.findViewById(R.id.user_name);
        textPassword = (TextView)v.findViewById(R.id.password_label);
        editPassword = (EditText)v.findViewById(R.id.password);

        switchAuthRequired.setOnCheckedChangeListener(this);
        editURL.addTextChangedListener(this);
        editUsername.addTextChangedListener(this);
        editPassword.addTextChangedListener(this);

        Uri createUri = activity.getIntent().getData();
        if (createUri != null)
            editURL.setText(createUri.toString());

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_enter_url, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem itemNext = menu.findItem(R.id.next);
        itemNext.setEnabled(
                activity.url != null &&
                        (!activity.authRequired || (StringUtils.isNotBlank(activity.username) && StringUtils.isNotBlank(activity.password)))
        );
    }


    /* dynamic changes */

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            activity.authRequired = true;
            textUsername.setVisibility(View.VISIBLE);
            editUsername.setVisibility(View.VISIBLE);
            textPassword.setVisibility(View.VISIBLE);
            editPassword.setVisibility(View.VISIBLE);
        } else {
            activity.authRequired = false;
            textUsername.setVisibility(View.GONE);
            editUsername.setVisibility(View.GONE);
            textPassword.setVisibility(View.GONE);
            editPassword.setVisibility(View.GONE);
        }

        updateAuthFields();
        activity.invalidateOptionsMenu();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String urlString = editURL.getText().toString();
        if (urlString.startsWith("webcal://") || urlString.startsWith("webcals://")) {
            urlString = "http" + urlString.substring(6);
            editURL.setText(urlString);
        }

        activity.url = null;
        try {
            activity.url = new URL(urlString);
            String protocol = activity.url.getProtocol();
            if ((!"http".equals(protocol) && !"https".equals(protocol) || StringUtils.isBlank(activity.url.getAuthority())))
                activity.url = null;
        } catch (MalformedURLException e) {
            Log.d(TAG, "Invalid URL", e);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        editURL.setTextColor(getResources().getColor(activity.url != null ?
                R.color.secondary_text_default_material_light :
                R.color.error_color));

        updateAuthFields();
        activity.invalidateOptionsMenu();
    }

    void updateAuthFields() {
        if (activity.authRequired) {
            insecureAuthWarning.setVisibility(activity.url != null && "https".equals(activity.url.getProtocol()) ? View.GONE : View.VISIBLE);
            activity.username = editUsername.getText().toString();
            activity.password = editPassword.getText().toString();
        } else {
            insecureAuthWarning.setVisibility(View.GONE);
            activity.username = null;
            activity.password = null;
        }
    }


    /* actions */

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.next) {
            ValidateCalendarFragment frag = new ValidateCalendarFragment();
            frag.show(getFragmentManager(), null);
            return true;
        }
        return false;
    }

}
