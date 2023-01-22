/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import at.bitfire.icsdroid.PermissionUtils
import at.bitfire.icsdroid.db.entity.Subscription

@Deprecated("Use Jetpack Compose")
class AddCalendarActivity: AppCompatActivity() {

    companion object {
        @Deprecated(
            "Use MainActivity",
            replaceWith = ReplaceWith(
                "EXTRA_TITLE",
                "at.bitfire.icsdroid.ui.activity.MainActivity.EXTRA_TITLE",
            ),
        )
        const val EXTRA_TITLE = "title"

        @Deprecated(
            "Use MainActivity",
            replaceWith = ReplaceWith(
                "EXTRA_COLOR",
                "at.bitfire.icsdroid.ui.activity.MainActivity.EXTRA_COLOR",
            ),
        )
        const val EXTRA_COLOR = "color"
    }

    private val titleColorModel by viewModels<TitleColorFragment.TitleColorModel>()


    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (!PermissionUtils.haveCalendarPermissions(this)) {
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
                    titleColorModel.color.value = getIntExtra(EXTRA_COLOR, Subscription.DEFAULT_COLOR)
            }
        }
    }

}
