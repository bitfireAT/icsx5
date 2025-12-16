/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.ui

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.dataStore
import at.bitfire.icsdroid.ui.icons.ModeCoolOff
import io.github.vinceglb.confettikit.compose.ConfettiKit
import io.github.vinceglb.confettikit.core.Angle
import io.github.vinceglb.confettikit.core.Party
import io.github.vinceglb.confettikit.core.Position
import io.github.vinceglb.confettikit.core.Rotation
import io.github.vinceglb.confettikit.core.Spread
import io.github.vinceglb.confettikit.core.emitter.Emitter
import io.github.vinceglb.confettikit.core.models.Shape
import io.github.vinceglb.confettikit.core.models.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.MonthDay
import kotlin.time.Duration

private val hideWinterEasterEgg = booleanPreferencesKey("hideWinterEasterEgg")

fun Context.hideWinterEasterEggFlow(): Flow<Boolean> = dataStore.data.map { it[hideWinterEasterEgg] ?: false }
suspend fun Context.hideWinterEasterEgg(hide: Boolean) {
    // save setting
    dataStore.edit { it[hideWinterEasterEgg] = hide }
}

/**
 * Determines whether the winter easter egg should be displayed.
 *
 * It is displayed from December 20th to December 31st.
 */
fun displayWinterEasterEgg(): Boolean {
    // return true // Uncomment for testing
    val now = MonthDay.now()
    return now >= MonthDay.of(12, 20) && now <= MonthDay.of(12, 31)
}

@Composable
fun WinterEasterEggToggleButton() {
    val shouldDisplay = remember { displayWinterEasterEgg() }
    if (!shouldDisplay) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val easterEggDisabled by context.hideWinterEasterEggFlow().collectAsState(false)

    IconButton(
        onClick = {
            scope.launch(Dispatchers.IO) {
                context.hideWinterEasterEgg(!easterEggDisabled)
            }
        }
    ) {
        Icon(
            if (easterEggDisabled) Icons.Rounded.AcUnit else Icons.Rounded.ModeCoolOff,
            stringResource(if (easterEggDisabled) R.string.winter_easter_egg_enable else R.string.winter_easter_egg_disable)
        )
    }
}

@Composable
fun WinterEasterEgg() {
    val shouldDisplay = remember { displayWinterEasterEgg() }
    if (!shouldDisplay) return

    val context = LocalContext.current
    val easterEggDisabled by context.hideWinterEasterEggFlow().collectAsState(false)
    if (easterEggDisabled) return

    ConfettiKit(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(999f),
        parties = listOf(
            Party(
                speed = 0f,
                maxSpeed = 10f,
                damping = 0.3f,
                size = listOf(Size(20, mass = 2f)),
                angle = Angle.BOTTOM,
                spread = Spread.ROUND,
                colors = listOf(0xb9d0eb, 0xa4abb3, 0x85b8f2),
                emitter = Emitter(duration = Duration.INFINITE).perSecond(3),
                position = Position.Relative(0.0, 0.0).between(Position.Relative(1.0, 0.0)),
                shapes = listOf(
                    Shape.Vector(rememberVectorPainter(Icons.Default.AcUnit))
                ),
                rotation = Rotation.disabled(),
                fadeOutEnabled = false,
                timeToLive = 15_000
            )
        )
    )
}
