/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import at.bitfire.icsdroid.PermissionUtils
import at.bitfire.icsdroid.db.LocalCalendar

class AddCalendarActivity: AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_COLOR = "color"
    }

    private val titleColorModel by viewModels<TitleColorFragment.TitleColorModel>()


    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val calendarPermissionRequestLauncher = PermissionUtils(this).getCalendarPermissionRequestLauncher()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED)
            calendarPermissionRequestLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))

        if (inState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .add(android.R.id.content, AddCalendarEnterUrlFragment())
                    .commit()

            intent?.apply {
                data?.let { uri ->
                    titleColorModel.url.value = uri.toString()
                }
                getStringExtra(EXTRA_TITLE)?.let {
                    titleColorModel.title.value = it
                }
                if (hasExtra(EXTRA_COLOR))
                    titleColorModel.color.value = getIntExtra(EXTRA_COLOR, LocalCalendar.DEFAULT_COLOR)
            }
        }
    }

}
