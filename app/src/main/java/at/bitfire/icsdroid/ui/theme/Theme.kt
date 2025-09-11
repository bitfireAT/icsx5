/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import at.bitfire.icsdroid.model.ThemeModel
import at.bitfire.icsdroid.ui.ForegroundTracker

private val DarkColors = darkColorScheme(
    primary = lightblue,
    onPrimary = offwhite,
    primaryContainer = lightblue,
    onPrimaryContainer = offwhite,
    secondary = lightblue,
    onSecondary = offwhite,
    secondaryContainer = lightblue,
    onSecondaryContainer = offwhite,
    tertiary = lightblue,
    onTertiary = offwhite,
    tertiaryContainer = lightblue,
    onTertiaryContainer = offwhite,
)

private val LightColors = lightColorScheme(
    primary = lightblue,
    onPrimary = offwhite,
    primaryContainer = lightblue,
    onPrimaryContainer = offwhite,
    secondary = lightblue,
    onSecondary = offwhite,
    secondaryContainer = lightblue,
    onSecondaryContainer = offwhite,
    tertiary = lightblue,
    onTertiary = offwhite,
    tertiaryContainer = lightblue,
    onTertiaryContainer = offwhite,
    background = offwhite,
    surfaceContainer = superlightblue,
    surface = superlightblue,
    surfaceContainerLowest = superlightblue,
    surfaceContainerLow = superlightblue,
    surfaceContainerHigh = superlightblue,
    surfaceContainerHighest = superlightblue,
    surfaceVariant = lightgrey,
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )

    // Track whether the app is in the foreground
    val view = LocalView.current
    LifecycleResumeEffect(view) {
        ForegroundTracker.onResume()
        onPauseOrDispose {
            ForegroundTracker.onPaused()
        }
    }
}

/**
 * Composes the given composable into the given activity. The content will become the root view of
 * the given activity.
 * This is roughly equivalent to calling [ComponentActivity.setContentView] with a ComposeView i.e.:
 * ```kotlin
 * setContentView(
 *   ComposeView(this).apply {
 *     setContent {
 *       MyComposableContent()
 *     }
 *   }
 * )
 * ```
 *
 * Then, applies [AppTheme] to the UI.
 *
 * @param parent The parent composition reference to coordinate scheduling of composition updates
 * @param darkTheme Calculates whether the UI should be shown in light or dark theme.
 * @param content A `@Composable` function declaring the UI contents
 */
fun ComponentActivity.setContentThemed(
    parent: CompositionContext? = null,
    darkTheme: @Composable () -> Boolean = {
        val model = hiltViewModel<ThemeModel>()
        val forceDarkTheme by model.forceDarkMode.collectAsState()
        forceDarkTheme || isSystemInDarkTheme()
    },
    content: @Composable () -> Unit
) {
    setContent(parent) {
        AppTheme(darkTheme = darkTheme()) {
            content()
        }
    }
}
