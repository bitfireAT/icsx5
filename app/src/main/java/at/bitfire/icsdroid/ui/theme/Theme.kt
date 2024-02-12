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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import at.bitfire.icsdroid.Settings

private val LightColors = lightColors(
    primary = colorPrimary,
    secondary = colorSecondary,
    onSecondary = Color.White
)

private val DarkColors = darkColors(
    primary = colorPrimaryDark,
    secondary = colorSecondary,
    onSecondary = Color.White
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colors = if (darkTheme)
        DarkColors
    else
        LightColors

    MaterialTheme(
        colors = colors
    ) {
        LaunchedEffect(darkTheme) {
            (context as? AppCompatActivity)?.let { activity ->
                val style = if (darkTheme)
                    SystemBarStyle.dark(
                        actionBarDarkTheme.toArgb()
                    )
                else
                    SystemBarStyle.dark(
                        colorPrimaryDark.toArgb()
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
        val forceDarkTheme by Settings(this).forceDarkModeLive().observeAsState()
        forceDarkTheme == true || isSystemInDarkTheme()
    },
    content: @Composable () -> Unit
) {
    setContent(parent) {
        AppTheme(darkTheme = darkTheme()) {
            content()
        }
    }
}
