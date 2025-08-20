/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.model.AddSubscriptionModel
import at.bitfire.icsdroid.ui.ResourceInfo
import at.bitfire.icsdroid.ui.partials.ExtendedTopAppBar
import at.bitfire.icsdroid.ui.theme.lightblue
import at.bitfire.icsdroid.ui.views.EnterUrlComposable
import at.bitfire.icsdroid.ui.views.SubscriptionSettingsComposable
import kotlinx.coroutines.launch

@Composable
fun AddSubscriptionScreen(
    title: String?,
    color: Int?,
    url: String?,
    model: AddSubscriptionModel = hiltViewModel(),
    onBackRequested: () -> Unit
) {
    val context = LocalContext.current
    val uiState = model.uiState

    LaunchedEffect(uiState) {
        if (uiState.success) {
            // on success, show notification and close activity
            Toast.makeText(context, context.getString(R.string.add_calendar_created), Toast.LENGTH_LONG).show()
            onBackRequested()
        }
        uiState.errorMessage?.let {
            // on error, show error message
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        title?.let(model.subscriptionSettingsUseCase::setTitle)
        color?.let(model.subscriptionSettingsUseCase::setColor)
        url?.let {
            model.subscriptionSettingsUseCase.setUrl(it)
            model.checkUrlIntroductionPage()
        }
    }

    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // keep the picked file accessible after the first sync and reboots
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            model.subscriptionSettingsUseCase.setUrl(uri.toString())

            // Get file name
            val displayName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val name = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.getString(name)
            }
            model.subscriptionSettingsUseCase.setFileName(displayName)
        }
    }

    Box(modifier = Modifier.imePadding()) {
        AddSubscriptionScreen(
            onPickFileRequested = { pickFile.launch(arrayOf("text/calendar")) },
            finish = onBackRequested,
            checkUrlIntroductionPage = model::checkUrlIntroductionPage
        )
    }
}

@Composable
fun AddSubscriptionScreen(
    onPickFileRequested: () -> Unit,
    checkUrlIntroductionPage: () -> Unit,
    finish: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { 2 }

    val model: AddSubscriptionModel = hiltViewModel()

    // Receive updates for the URL introduction page
    with(model.subscriptionSettingsUseCase.uiState) {
        LaunchedEffect(url, requiresAuth, username, password) {
            checkUrlIntroductionPage()
        }
    }

    // Receive updates for the Details page
    with(model.subscriptionSettingsUseCase.uiState) {
        LaunchedEffect(title, color, ignoreAlerts, defaultAlarmMinutes, defaultAllDayAlarmMinutes) {
            model.setShowNextButton(!title.isNullOrBlank())
        }
    }

    val isVerifyingUrl = model.validationUseCase.uiState.isVerifyingUrl
    val validationResult = model.validationUseCase.uiState.result

    LaunchedEffect(validationResult) {
        Log.i("AddCalendarActivity", "Validation result updated: $validationResult")
        validationResult?.let { info ->
            if (info.exception != null)
                return@LaunchedEffect

            // When a result has been obtained, and it's neither null nor has an exception,
            // clean the subscriptionSettingsModel, and move the pager to the next page
            with(model.subscriptionSettingsUseCase) {
                setUrl(info.uri.toString())

                if (uiState.color == null)
                    setColor(info.calendarColor ?: lightblue.toArgb())

                if (uiState.title.isNullOrBlank())
                    setTitle(info.calendarName ?: info.uri.toString())
            }

            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        }
    }
    with(model.subscriptionSettingsUseCase) {
        AddSubscriptionScreen(
            pagerState = pagerState,
            requiresAuth = uiState.requiresAuth,
            onRequiresAuthChange = ::setRequiresAuth,
            username = uiState.username,
            onUsernameChange = ::setUsername,
            password = uiState.password,
            onPasswordChange = ::setPassword,
            isInsecure = uiState.isInsecure,
            url = uiState.url,
            onUrlChange = {
                setUrl(it)
                setFileName(null)
            },
            fileName = uiState.fileName,
            urlError = uiState.urlError,
            supportsAuthentication = uiState.supportsAuthentication,
            title = uiState.title,
            onTitleChange = ::setTitle,
            color = uiState.color,
            onColorChange = ::setColor,
            customUserAgent = uiState.customUserAgent,
            onCustomUserAgentChange = ::setCustomUserAgent,
            ignoreAlerts = uiState.ignoreAlerts,
            onIgnoreAlertsChange = ::setIgnoreAlerts,
            defaultAlarmMinutes = uiState.defaultAlarmMinutes,
            onDefaultAlarmMinutesChange = ::setDefaultAlarmMinutes,
            defaultAllDayAlarmMinutes = uiState.defaultAllDayAlarmMinutes,
            onDefaultAllDayAlarmMinutesChange = ::setDefaultAllDayAlarmMinutes,
            ignoreDescription = uiState.ignoreDescription,
            onIgnoreDescriptionChange = ::setIgnoreDescription,
            showNextButton = model.uiState.showNextButton,
            isVerifyingUrl = isVerifyingUrl,
            isCreating = model.uiState.isCreating,
            validationResult = validationResult,
            onResetResult = model::resetValidationResult,
            onPickFileRequested = onPickFileRequested,
            onNextRequested = { page: Int ->
                when (page) {
                    // First page (Enter Url)
                    0 -> {
                        // flush the credentials if auth toggle is disabled
                        if (!uiState.requiresAuth)
                            clearCredentials()

                        val uri: Uri? = uiState.url?.let(Uri::parse)
                        val authenticate = uiState.requiresAuth

                        if (uri != null) {
                            model.validateUrl(
                                originalUri = uri,
                                username = if (authenticate) uiState.username else null,
                                password = if (authenticate) uiState.password else null
                            )
                        }
                    }
                    // Second page (details and confirm)
                    1 -> {
                        model.createSubscription()
                    }
                }
            },
            onNavigationClicked = {
                // If first page, close activity
                if (pagerState.currentPage <= 0) finish()
                // otherwise, go back a page
                else scope.launch {
                    // Needed for non-first-time validations to trigger following validation result updates
                    model.resetValidationResult()
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                }
            }
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun AddSubscriptionScreen(
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
    customUserAgent: String?,
    onCustomUserAgentChange: (String) -> Unit,
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
                    onSubmit = { onNextRequested(0) }
                )

                1 -> SubscriptionSettingsComposable(
                    url = url,
                    title = title,
                    titleChanged = onTitleChange,
                    color = color,
                    colorChanged = onColorChange,
                    customUserAgent = customUserAgent,
                    customUserAgentChanged = onCustomUserAgentChange,
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
