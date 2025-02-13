package at.bitfire.icsdroid.ui.theme

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import at.bitfire.icsdroid.model.ThemeModel

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
    val context = LocalContext.current

    val colorScheme = if (darkTheme)
        DarkColors
    else
        LightColors

    MaterialTheme(colorScheme = colorScheme) {
        LaunchedEffect(darkTheme) {
            (context as? AppCompatActivity)?.let { activity ->
                val style = if (darkTheme)
                    SystemBarStyle.dark(
                        nearlyBlack.toArgb()
                    )
                else
                    SystemBarStyle.dark(
                        darkblue.toArgb()
                    )
                activity.enableEdgeToEdge(
                    statusBarStyle = style,
                    navigationBarStyle = style
                )
            } ?: Log.e("AppTheme", "Context is not activity!")
        }

        Box(
            modifier = Modifier
                // Required to make sure all paddings are correctly set
                .systemBarsPadding()
                .fillMaxSize()
        ) {
            content()
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
        val model = viewModel<ThemeModel>()
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
