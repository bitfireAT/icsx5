package at.bitfire.icsdroid.ui.screen

import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.model.CreateSubscriptionModel
import at.bitfire.icsdroid.model.CredentialsModel
import at.bitfire.icsdroid.model.SubscriptionSettingsModel
import at.bitfire.icsdroid.model.SubscriptionsModel
import at.bitfire.icsdroid.model.ValidationModel
import at.bitfire.icsdroid.ui.ResourceInfo
import at.bitfire.icsdroid.ui.partials.ExtendedTopAppBar
import at.bitfire.icsdroid.ui.theme.lightblue
import at.bitfire.icsdroid.ui.views.EnterUrlComposable
import at.bitfire.icsdroid.ui.views.SubscriptionSettingsComposable
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun AddCalendarScreen(
    createSubscriptionModel: CreateSubscriptionModel,
    subscriptionSettingsModel: SubscriptionSettingsModel,
    credentialsModel: CredentialsModel,
    validationModel: ValidationModel,
    onPickFileRequested: () -> Unit,
    checkUrlIntroductionPage: () -> Unit,
    finish: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { 2 }

    // Receive updates for the URL introduction page
    LaunchedEffect(
        subscriptionSettingsModel.uiState.url,
        credentialsModel.uiState.requiresAuth,
        credentialsModel.uiState.username,
        credentialsModel.uiState.password,
        validationModel.uiState.isVerifyingUrl
    ) {
        checkUrlIntroductionPage()
    }

    // Receive updates for the Details page
    LaunchedEffect(
        subscriptionSettingsModel.uiState.title,
        subscriptionSettingsModel.uiState.color,
        subscriptionSettingsModel.uiState.ignoreAlerts,
        subscriptionSettingsModel.uiState.defaultAlarmMinutes,
        subscriptionSettingsModel.uiState.defaultAllDayAlarmMinutes
    ) {
        createSubscriptionModel.setShowNextButton(
            !subscriptionSettingsModel.uiState.title.isNullOrBlank()
        )
    }

    LaunchedEffect(validationModel.uiState.result) {
        val validationResult = validationModel.uiState.result
        Log.i("AddCalendarActivity", "Validation result updated: $validationResult")
        if (validationResult == null || validationResult.exception != null) return@LaunchedEffect
        val info = validationResult

        // When a result has been obtained, and it's neither null nor has an exception,
        // clean the subscriptionSettingsModel, and move the pager to the next page
        subscriptionSettingsModel.setUrl(info.uri.toString())

        if (subscriptionSettingsModel.uiState.color == null)
            subscriptionSettingsModel.setColor(info.calendarColor ?: lightblue.toArgb())

        if (subscriptionSettingsModel.uiState.title.isNullOrBlank())
            subscriptionSettingsModel.setTitle(info.calendarName ?: info.uri.toString())

        pagerState.animateScrollToPage(pagerState.currentPage + 1)
    }

    AddCalendarScreen(
        pagerState = pagerState,
        requiresAuth = credentialsModel.uiState.requiresAuth,
        onRequiresAuthChange = credentialsModel::setRequiresAuth,
        username = credentialsModel.uiState.username,
        onUsernameChange = credentialsModel::setUsername,
        password = credentialsModel.uiState.password,
        onPasswordChange = credentialsModel::setPassword,
        isInsecure = credentialsModel.uiState.isInsecure,
        url = subscriptionSettingsModel.uiState.url,
        onUrlChange = {
            subscriptionSettingsModel.setUrl(it)
            subscriptionSettingsModel.setFileName(null)
        },
        fileName = subscriptionSettingsModel.uiState.fileName,
        urlError = subscriptionSettingsModel.uiState.urlError,
        supportsAuthentication = subscriptionSettingsModel.uiState.supportsAuthentication,
        title = subscriptionSettingsModel.uiState.title,
        onTitleChange = subscriptionSettingsModel::setTitle,
        color = subscriptionSettingsModel.uiState.color,
        onColorChange = subscriptionSettingsModel::setColor,
        ignoreAlerts = subscriptionSettingsModel.uiState.ignoreAlerts,
        onIgnoreAlertsChange = subscriptionSettingsModel::setIgnoreAlerts,
        defaultAlarmMinutes = subscriptionSettingsModel.uiState.defaultAlarmMinutes,
        onDefaultAlarmMinutesChange = subscriptionSettingsModel::setDefaultAlarmMinutes,
        defaultAllDayAlarmMinutes = subscriptionSettingsModel.uiState.defaultAllDayAlarmMinutes,
        onDefaultAllDayAlarmMinutesChange = subscriptionSettingsModel::setDefaultAllDayAlarmMinutes,
        ignoreDescription = subscriptionSettingsModel.uiState.ignoreDescription,
        onIgnoreDescriptionChange = subscriptionSettingsModel::setIgnoreDescription,
        showNextButton = createSubscriptionModel.uiState.showNextButton,
        isVerifyingUrl = validationModel.uiState.isVerifyingUrl,
        isCreating = createSubscriptionModel.uiState.isCreating,
        validationResult = validationModel.uiState.result,
        onResetResult = validationModel::resetResult,
        onPickFileRequested = onPickFileRequested,
        onNextRequested = { page: Int ->
            when (page) {
                // First page (Enter Url)
                0 -> {
                    // flush the credentials if auth toggle is disabled
                    if (!credentialsModel.uiState.requiresAuth) {
                        credentialsModel.clearCredentials()
                    }

                    val uri: Uri? = subscriptionSettingsModel.uiState.url?.let(Uri::parse)
                    val authenticate = credentialsModel.uiState.requiresAuth

                    if (uri != null) {
                        validationModel.validate(
                            uri,
                            if (authenticate) credentialsModel.uiState.username else null,
                            if (authenticate) credentialsModel.uiState.password else null
                        )
                    }
                }
                // Second page (details and confirm)
                1 -> {
                    createSubscriptionModel.create(subscriptionSettingsModel, credentialsModel)
                }
            }
        },
        onNavigationClicked = {
            // If first page, close activity
            if (pagerState.currentPage <= 0) finish()
            // otherwise, go back a page
            else scope.launch {
                // Needed for non-first-time validations to trigger following validation result updates
                validationModel.resetResult()
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            }
        }
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun AddCalendarScreen(
    pagerState: PagerState = rememberPagerState { 2 },
    requiresAuth: Boolean,
    onRequiresAuthChange: (Boolean) -> Unit,
    username: String?,
    onUsernameChange: (String) -> Unit,
    password: String?,
    onPasswordChange: (String) -> Unit,
    isInsecure: Boolean,
    url: String?,
    fileName: String?,
    onUrlChange: (String?) -> Unit,
    urlError: String?,
    supportsAuthentication: Boolean,
    title: String?,
    onTitleChange: (String) -> Unit,
    color: Int?,
    onColorChange: (Int) -> Unit,
    ignoreAlerts: Boolean,
    onIgnoreAlertsChange: (Boolean) -> Unit,
    defaultAlarmMinutes: Long?,
    onDefaultAlarmMinutesChange: (String) -> Unit,
    defaultAllDayAlarmMinutes: Long?,
    onDefaultAllDayAlarmMinutesChange: (String) -> Unit,
    ignoreDescription: Boolean,
    onIgnoreDescriptionChange: (Boolean) -> Unit,
    showNextButton: Boolean,
    isVerifyingUrl: Boolean,
    isCreating: Boolean,
    validationResult: ResourceInfo?,
    onResetResult: () -> Unit,
    onPickFileRequested: () -> Unit,
    onNextRequested: (page: Int) -> Unit,
    onNavigationClicked: () -> Unit
) {
    Scaffold(
        topBar = {
            AddCalendarTopAppBar(pagerState, showNextButton, isVerifyingUrl, isCreating, onNextRequested, onNavigationClicked)
        },
        bottomBar = {
            AddCalendarBottomAppBar(pagerState, showNextButton, isVerifyingUrl, isCreating, onNextRequested)
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (page) {
                0 -> EnterUrlComposable(
                    requiresAuth = requiresAuth,
                    onRequiresAuthChange = onRequiresAuthChange,
                    username = username,
                    onUsernameChange = onUsernameChange,
                    password = password,
                    onPasswordChange = onPasswordChange,
                    isInsecure = isInsecure,
                    url = url,
                    fileName = fileName,
                    onUrlChange = onUrlChange,
                    urlError = urlError,
                    supportsAuthentication = supportsAuthentication,
                    isVerifyingUrl = isVerifyingUrl,
                    validationResult = validationResult,
                    onValidationResultDismiss = onResetResult,
                    onPickFileRequested = onPickFileRequested,
                    onSubmit = { onNextRequested(1) }
                )

                1 -> SubscriptionSettingsComposable(
                    url = url,
                    title = title,
                    titleChanged = onTitleChange,
                    color = color,
                    colorChanged = onColorChange,
                    ignoreAlerts = ignoreAlerts,
                    ignoreAlertsChanged = onIgnoreAlertsChange,
                    defaultAlarmMinutes = defaultAlarmMinutes,
                    defaultAlarmMinutesChanged = onDefaultAlarmMinutesChange,
                    defaultAllDayAlarmMinutes = defaultAllDayAlarmMinutes,
                    defaultAllDayAlarmMinutesChanged = onDefaultAllDayAlarmMinutesChange,
                    ignoreDescription = ignoreDescription,
                    onIgnoreDescriptionChanged = onIgnoreDescriptionChange,
                    isCreating = isCreating,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                )
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
private fun AddCalendarTopAppBar(
    pagerState: PagerState,
    showNextButton: Boolean,
    isVerifyingUrl: Boolean,
    isCreating: Boolean,
    onNextRequested: (Int) -> Unit,
    onNavigationClicked: () -> Unit
) {
    ExtendedTopAppBar(
        navigationIcon = {
            IconButton(
                onClick = onNavigationClicked
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
        },
        title = { Text(text = stringResource(R.string.activity_add_calendar)) },
        actions = {
            AnimatedVisibility(visible = showNextButton) {
                IconButton(
                    onClick = { onNextRequested(pagerState.currentPage) },
                    enabled = !isVerifyingUrl && !isCreating
                ) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null) }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddCalendarBottomAppBar(
    pagerState: PagerState,
    showNextButton: Boolean,
    isVerifyingUrl: Boolean,
    isCreating: Boolean,
    onNextRequested: (Int) -> Unit
) {
    AnimatedVisibility(
        pagerState.currentPage == 0 && showNextButton,
        enter = expandVertically()
    ) {
        BottomAppBar(
            content = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onNextRequested(pagerState.currentPage) },
                        enabled = !isVerifyingUrl && !isCreating
                    ) {
                        Text(stringResource(R.string.activity_add_calendar_subscribe).uppercase())
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            stringResource(R.string.activity_add_calendar_subscribe)
                        )
                    }
                }
            }
        )
    }
}
