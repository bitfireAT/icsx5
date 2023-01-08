/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import at.bitfire.icsdroid.db.entity.Subscription
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener

@Deprecated("Use Jetpack Compose")
class ColorPickerActivity: AppCompatActivity(), ColorPickerDialogListener {

    companion object {
        const val EXTRA_COLOR = "color"
    }

    class Contract: ActivityResultContract<Int?, Int>() {
        override fun createIntent(context: Context, input: Int?): Intent = Intent(context, ColorPickerActivity::class.java).apply {
            putExtra(EXTRA_COLOR, input)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Int = intent?.getIntExtra(EXTRA_COLOR, Subscription.DEFAULT_COLOR) ?: Subscription.DEFAULT_COLOR
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val builder = ColorPickerDialog.newBuilder()
                    .setShowAlphaSlider(false)
                    .setAllowCustom(true)

            intent?.apply {
                if (hasExtra(EXTRA_COLOR))
                    builder.setColor(getIntExtra(EXTRA_COLOR, Subscription.DEFAULT_COLOR))
            }

            val dialog = builder.create()
            dialog.show(supportFragmentManager, "color")
        }
    }

    override fun onColorSelected(dialogId: Int, color: Int) {
        val result = Intent()
        result.putExtra(EXTRA_COLOR, color)
        setResult(0, result)
        finish()
    }

    override fun onDialogDismissed(dialogId: Int) {
        finish()
    }

}