package at.bitfire.icsdroid.ui.nav

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import at.bitfire.icsdroid.ui.screen.EditSubscriptionScreen
import at.bitfire.icsdroid.ui.screen.SubscriptionsScreen
import java.util.ServiceLoader

@Composable
fun MainApp(
    savedInstanceState: Bundle?,
    intentExtras: Bundle?,
) {
    val backStack = rememberNavBackStack<Destination>(Destination.SubscriptionList)

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

    NavDisplay(
        entryDecorators = listOf(
            // Add the default decorators for managing scenes and saving state
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
            // Then add the view model store decorator
            rememberViewModelStoreNavEntryDecorator()
        ),
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Destination.SubscriptionList> {
                SubscriptionsScreen(
                    requestPermissions,
                    onItemSelected = {
                        backStack.add(Destination.EditSubscription(it.id))
                    }
                )
            }
            entry<Destination.EditSubscription> { destination ->
                EditSubscriptionScreen(
                    subscriptionId = destination.subscriptionId,
                    onBackRequested = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}
