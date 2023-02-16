/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import at.bitfire.icsdroid.PermissionUtils
import at.bitfire.icsdroid.db.LocalCalendar

class AddCalendarActivity: AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_COLOR = "color"

        const val EXTRA_PERMISSION = "permission"
    }

    private val titleColorModel by viewModels<TitleColorFragment.TitleColorModel>()


    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val requestPermissions = intent.getBooleanExtra(EXTRA_PERMISSION, false)
        if (requestPermissions && !PermissionUtils.haveCalendarPermissions(this)) {
            PermissionUtils
                .registerCalendarPermissionRequest(this)()
        }

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
