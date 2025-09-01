/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.ui.nav

import android.accounts.AccountManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import at.bitfire.icsdroid.service.ComposableStartupService
import at.bitfire.icsdroid.ui.partials.AlertDialog
import at.bitfire.icsdroid.ui.screen.AddSubscriptionScreen
import at.bitfire.icsdroid.ui.screen.InfoScreen
import at.bitfire.icsdroid.ui.screen.SubscriptionsScreen
import java.util.ServiceLoader

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
                    onAboutRequested = { backStack.add(Destination.Info) },
                    onAddRequested = { backStack.add(Destination.AddSubscription()) }
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
        }
    )
}
