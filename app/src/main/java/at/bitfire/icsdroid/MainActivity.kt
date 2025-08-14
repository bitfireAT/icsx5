/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import at.bitfire.icsdroid.MainActivity.Companion.EXTRA_ERROR_MESSAGE
import at.bitfire.icsdroid.MainActivity.Companion.EXTRA_THROWABLE
import at.bitfire.icsdroid.ui.nav.MainApp
import at.bitfire.icsdroid.ui.theme.setContentThemed
import at.bitfire.icsdroid.ui.views.configureEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        configureEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContentThemed {
            MainApp(savedInstanceState, intent.extras)
        }
    }

    companion object {
        /**
         * Set this extra to request calendar permission when the activity starts.
         */
        const val EXTRA_REQUEST_CALENDAR_PERMISSION = "permission"

        const val PRIVACY_POLICY_URL = "https://icsx5.bitfire.at/privacy/"

        /**
         * If set, an alert dialog will be displayed with the message of this error.
         * May be set together with [EXTRA_THROWABLE] to display a stack trace.
         */
        const val EXTRA_ERROR_MESSAGE = "errorMessage"

        /**
         * If set, an alert dialog will be displayed with the stack trace of this error.
         * If set, [EXTRA_ERROR_MESSAGE] must also be set, otherwise does nothing.
         */
        const val EXTRA_THROWABLE = "errorThrowable"
    }

}
