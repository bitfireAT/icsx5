package at.bitfire.icsdroid.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import at.bitfire.icsdroid.ui.theme.setContentThemed

class MainActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentThemed {

        }
    }
}
