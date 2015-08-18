package at.bitfire.icsdroid;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.net.URL;

public class AddAccountActivity extends AppCompatActivity {
    URL url;
    boolean authRequired = false;
    String username, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_account);

        if (savedInstanceState == null)
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, new EnterUrlFragment(), "enter_url")
                    .commit();
    }


}
