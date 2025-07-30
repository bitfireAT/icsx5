/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.service

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/**
 * Used for interactions between flavors.
 *
 * Provides the possibility to display some composable (intended for dialogs) if a given condition
 * is met.
 */
interface ComposableStartupService {
    companion object {
        /**
         * Tag services with this flag to tell the application that they are a donation dialog, and
         * they will be considered in `InfoActivity` to donate for example.
         */
        const val FLAG_DONATION_DIALOG = 0b1
    }

    /**
     * A bitwise flag that can be used to identify this service.
     * Currently only [FLAG_DONATION_DIALOG] is supported.
     */
    val flags: Int

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

    /**
     * Checks whether [flags] contains the given [flag] using bitwise operations.
     */
    fun hasFlag(flag: Int) = (flags and flag) == flag
}
