package at.bitfire.icsdroid.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import at.bitfire.icsdroid.Constants.TAG
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.ui.activity.MainActivity.Companion.Paths
import at.bitfire.icsdroid.ui.model.CreateSubscriptionModel
import at.bitfire.icsdroid.ui.pages.CreateSubscriptionSelectPage
import at.bitfire.icsdroid.ui.pages.CreateSubscriptionValidationPage
import at.bitfire.icsdroid.ui.pages.SubscriptionDetailsPage
import at.bitfire.icsdroid.utils.toast
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Composable
@ExperimentalPagerApi
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
fun CreateSubscription(navHostController: NavHostController, model: CreateSubscriptionModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var page by model.page

    var selectedTab by model.selectionType
    val isValid by model.isValid
    val displayName by model.displayName
    val fieldsEnabled by model.fieldsEnabled

    val selectionPagerState = rememberPagerState()
    LaunchedEffect(selectionPagerState) {
        snapshotFlow { selectionPagerState.currentPage }.collect { selectedTab = it }
    }

    val pagerState = rememberPagerState()
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect {
                page = it
                // If the validation page is selected, validate source
                if (it == 1) model.validation(
                    onSuccess = {
                        // The uri is fine, go to the next page
                        scope.launch { pagerState.animateScrollToPage(2) }
                    },
                    onError = { error ->
                        // There has been an issue with the uri
                        Log.e(TAG, "Uri is not valid.", error)
                        // Go to the previous page
                        scope.launch { pagerState.animateScrollToPage(0) }
                        // Show error to the user
                        // TODO: Show error to the user
                    },
                )
            }
    }

    fun onBack() {
        if (page == 0) {
            // If on the first page, go to the main screen
            Paths.Subscriptions.navigate(navHostController)
            model.dispose()
        } else if (page == 2) {
            // If on the third page, go back to the first one
            scope.launch { pagerState.scrollToPage(0) }
        }
        // Note, back pressing when validating does nothing
    }

    BackHandler(onBack = ::onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.activity_add_calendar)) },
                navigationIcon = {
                    IconButton(onClick = ::onBack) {
                        Icon(
                            Icons.Rounded.KeyboardArrowLeft,
                            stringResource(R.string.edit_calendar_cancel),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (isValid && page == 0)
                ExtendedFloatingActionButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(page + 1) } }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = stringResource(R.string.action_next),
                    )
                    Text(stringResource(R.string.action_next))
                }
            else if (page == 2 && displayName.isNotBlank())
                ExtendedFloatingActionButton(
                    onClick = {
                        model.fieldsEnabled.value = false
                        model.create().invokeOnCompletion { exception ->
                            runBlocking(Dispatchers.Main) {
                                if (exception != null) {
                                    Log.e(TAG, "Could not create subscription.", exception)
                                    exception.localizedMessage?.let { context.toast(it) }
                                } else {
                                    Log.i(TAG, "Created subscription successfully.")
                                    // Show toast to the user
                                    context.toast(R.string.add_calendar_created)
                                    // Navigate to the subscriptions screen
                                    Paths.Subscriptions.navigate(navHostController)
                                    // Dispose the form
                                    model.dispose()
                                }
                            }
                        }
                    },
                    containerColor = FloatingActionButtonDefaults.containerColor.copy(
                        alpha = if (fieldsEnabled) 1f else ContentAlpha.disabled,
                    ),
                    elevation = if (fieldsEnabled)
                        FloatingActionButtonDefaults.elevation()
                    else
                        FloatingActionButtonDefaults.loweredElevation(),
                ) {
                    if (fieldsEnabled)
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = stringResource(R.string.add_calendar_create),
                        )
                    else
                        CircularProgressIndicator()
                    AnimatedVisibility(visible = fieldsEnabled) {
                        Text(stringResource(R.string.add_calendar_create))
                    }
                }
        }
    ) { paddingValues ->
        HorizontalPager(
            count = 3,
            userScrollEnabled = false,
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
            ) {
                when (page) {
                    0 -> CreateSubscriptionSelectPage(
                        pagerState = selectionPagerState,
                        model = model,
                        selectedTab = selectedTab,
                        onTabSelected = { selectionPagerState.animateScrollToPage(it) },
                    )
                    1 -> CreateSubscriptionValidationPage()
                    2 -> SubscriptionDetailsPage(model)
                }
            }
        }
    }
}
