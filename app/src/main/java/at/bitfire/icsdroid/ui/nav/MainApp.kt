/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.ui.nav

import android.accounts.AccountManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ShareCompat
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
import at.bitfire.icsdroid.ui.screen.SubscriptionsScreen
import java.util.ServiceLoader

/**
 * Computes the correct initial destination from some intent extras:
 * - If [AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE] is present -> [Destination.AddSubscription]
 * - Otherwise: [Destination.SubscriptionList]
 */
private fun calculateInitialDestination(intentExtras: Bundle?): Destination {
    return if (intentExtras?.containsKey(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE) == true) {
        // If KEY_ACCOUNT_AUTHENTICATOR_RESPONSE was given, intent was launched from authenticator,
        // open the add subscription screen
        Destination.AddSubscription()
    } else {
        // If no condition matches, show the subscriptions list
        Destination.SubscriptionList
    }
}

@Composable
fun MainApp(
    savedInstanceState: Bundle?,
    intentExtras: Bundle?,
    onFinish: () -> Unit,
) {
    // If EXTRA_PERMISSION is true, request the calendar permissions
    val requestPermissions = intentExtras?.getBoolean(EXTRA_REQUEST_CALENDAR_PERMISSION, false) == true

    // show error message from calling intent, if available
    var showingErrorMessage by remember {
        mutableStateOf(savedInstanceState == null && intentExtras?.containsKey(EXTRA_ERROR_MESSAGE) == true)
    }
    if (showingErrorMessage && intentExtras != null) {
        AlertDialog(
            intentExtras.getString(EXTRA_ERROR_MESSAGE)!!,
            BundleCompat.getSerializable(intentExtras, EXTRA_THROWABLE, Throwable::class.java)
        ) { showingErrorMessage = false }
    }

    // Init and collect all ComposableStartupServices
    val compStartupServices = remember { ServiceLoader.load(ComposableStartupService::class.java) }

    compStartupServices.forEach { service ->
        val show: Boolean by service.shouldShow()
        if (show) service.Content()
    }

    val backStack = rememberNavBackStack(calculateInitialDestination(intentExtras))

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
                    onAddRequested = { backStack.add(Destination.AddSubscription()) },
                    onItemSelected = { backStack.add(Destination.EditSubscription(it.id)) }
                )
            }
            entry<Destination.AddSubscription> { destination ->
                var url: String? = null
                LaunchedEffect(intentExtras) {
                    if (intentExtras != null) {
                        intentExtras.getString(Intent.EXTRA_TEXT)
                            ?.trim()
                            ?.let { url = it }
                        BundleCompat.getParcelable(intentExtras, Intent.EXTRA_STREAM, Uri::class.java)
                            ?.toString()
                            ?.let { url = it }
                    }
                }

                AddSubscriptionScreen(
                    title = destination.title,
                    color = destination.color,
                    url = url,
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
