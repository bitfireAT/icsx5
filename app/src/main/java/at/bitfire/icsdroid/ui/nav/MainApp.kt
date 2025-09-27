/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.ui.nav

import android.accounts.AccountManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.core.app.ShareCompat
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import at.bitfire.icsdroid.MainActivity.Companion.EXTRA_ERROR_MESSAGE
import at.bitfire.icsdroid.MainActivity.Companion.EXTRA_REQUEST_CALENDAR_PERMISSION
import at.bitfire.icsdroid.MainActivity.Companion.EXTRA_THROWABLE
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.service.ComposableStartupService
import at.bitfire.icsdroid.ui.partials.AlertDialog
import at.bitfire.icsdroid.ui.screen.AddSubscriptionScreen
import at.bitfire.icsdroid.ui.screen.EditSubscriptionScreen
import at.bitfire.icsdroid.ui.screen.InfoScreen
import at.bitfire.icsdroid.ui.screen.SubscriptionsScreen
import io.github.vinceglb.confettikit.compose.ConfettiKit
import io.github.vinceglb.confettikit.core.Angle
import io.github.vinceglb.confettikit.core.Party
import io.github.vinceglb.confettikit.core.Position
import io.github.vinceglb.confettikit.core.Rotation
import io.github.vinceglb.confettikit.core.Spread
import io.github.vinceglb.confettikit.core.emitter.Emitter
import io.github.vinceglb.confettikit.core.models.Shape
import io.github.vinceglb.confettikit.core.models.Size
import java.util.ServiceLoader
import kotlin.time.Duration

/**
 * Computes the correct initial destination from some intent:
 * - If [AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE] is present -> [Destination.AddSubscription]
 * - Otherwise: [Destination.SubscriptionList]
 */
private fun calculateInitialDestination(intent: Intent): Destination {
    val extras = intent.extras ?: Bundle.EMPTY
    val text = extras.getString(Intent.EXTRA_TEXT)
        ?.trim()
        ?.takeUnless { it.isEmpty() }
    val stream = BundleCompat.getParcelable(extras, Intent.EXTRA_STREAM, Uri::class.java)
        ?.toString()
    val data = intent.dataString

    return if (extras.containsKey(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)) {
        // If KEY_ACCOUNT_AUTHENTICATOR_RESPONSE was given, intent was launched from authenticator,
        // open the add subscription screen
        Destination.AddSubscription()
    } else if (text != null || stream != null || data != null) {
        // If a URL was given, open the add subscription screen
        Destination.AddSubscription(
            title = extras.getString("title"),
            color = extras.takeIf { it.containsKey("color") }?.getInt("color", -1),
            url = text ?: stream ?: data
        )
    } else {
        // If no condition matches, show the subscriptions list
        Destination.SubscriptionList
    }
}

@Composable
fun MainApp(
    savedInstanceState: Bundle?,
    intent: Intent,
    onFinish: () -> Unit,
) {
    // If EXTRA_PERMISSION is true, request the calendar permissions
    val requestPermissions = intent.getBooleanExtra(EXTRA_REQUEST_CALENDAR_PERMISSION, false)

    // show error message from calling intent, if available
    var showingErrorMessage by remember {
        mutableStateOf(savedInstanceState == null && intent.hasExtra(EXTRA_ERROR_MESSAGE))
    }
    if (showingErrorMessage) {
        AlertDialog(
            intent.getStringExtra(EXTRA_ERROR_MESSAGE)!!,
            IntentCompat.getSerializableExtra(intent, EXTRA_THROWABLE, Throwable::class.java)
        ) { showingErrorMessage = false }
    }

    // Init and collect all ComposableStartupServices
    val compStartupServices = remember { ServiceLoader.load(ComposableStartupService::class.java) }

    compStartupServices.forEach { service ->
        val show: Boolean by service.shouldShow()
        if (show) service.Content()
    }

    val backStack = rememberNavBackStack(calculateInitialDestination(intent))

    fun goBack(depth: Int = 1) {
        if (backStack.size <= 1) onFinish()
        else repeat(depth) { backStack.removeAt(backStack.lastIndex) }
    }

    ConfettiKit(
        modifier = Modifier.fillMaxSize().zIndex(999f),
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

    NavDisplay(
        entryDecorators = listOf(
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        backStack = backStack,
        onBack = ::goBack,
        entryProvider = entryProvider {
            entry(Destination.SubscriptionList) {
                SubscriptionsScreen(
                    requestPermissions,
                    onAddRequested = { backStack.add(Destination.AddSubscription()) },
                    onItemSelected = { backStack.add(Destination.EditSubscription(it.id)) },
                    onAboutRequested = { backStack.add(Destination.Info) },
                )
            }
            entry(Destination.Info) {
                InfoScreen(
                    compStartupServices,
                    onBackRequested = ::goBack
                )
            }
            entry<Destination.AddSubscription> { destination ->
                AddSubscriptionScreen(
                    title = destination.title,
                    color = destination.color,
                    url = destination.url,
                    onBackRequested = { goBack() }
                )
            }
            entry<Destination.EditSubscription> { destination ->
                val context = LocalContext.current
                EditSubscriptionScreen(
                    subscriptionId = destination.subscriptionId,
                    onShare = { subscription ->
                        ShareCompat.IntentBuilder(context)
                            .setSubject(subscription.displayName)
                            .setText(subscription.url.toString())
                            .setType("text/plain")
                            .setChooserTitle(R.string.edit_calendar_send_url)
                            .startChooser()
                    },
                    onExit = ::goBack
                )
            }
        }
    )
}
