/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.service

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/**
 * Used for interactions between flavors.
 *
 * Provides the possibility to display some composable (intended for dialogs) if a given condition
 * is met.
 */
interface ComposableStartupService {
    /**
     * Will be called every time the main activity is created.
     * @param activity The calling activity
     */
    fun initialize(activity: AppCompatActivity)

    /**
     * Provides a stateful response to whether this composable should be shown or not.
     * @return A [State] that can be observed, and will make [Content] visible when `true`.
     */
    @Composable
    fun shouldShow(): State<Boolean>

    /**
     * The content to display. It's not constrained, will be rendered together with the main UI.
     * Usually an `AlertDialog`.
     */
    @Composable
    fun Content()
}
