/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.ui.views

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat

fun ComponentActivity.configureEdgeToEdge() {
    enableEdgeToEdge()
    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
}
