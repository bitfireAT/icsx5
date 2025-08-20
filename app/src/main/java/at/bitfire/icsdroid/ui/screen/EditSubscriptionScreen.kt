/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.model.EditSubscriptionModel
import at.bitfire.icsdroid.model.EditSubscriptionModel.EditSubscriptionModelFactory
import at.bitfire.icsdroid.ui.partials.ExtendedTopAppBar
import at.bitfire.icsdroid.ui.partials.GenericAlertDialog
import at.bitfire.icsdroid.ui.theme.AppTheme
import at.bitfire.icsdroid.ui.views.LoginCredentialsComposable
import at.bitfire.icsdroid.ui.views.SubscriptionSettingsComposable

@Composable
fun EditSubscriptionScreen(
    subscriptionId: Long,
    onShare: (subscription: Subscription) -> Unit,
    onExit: () -> Unit = {}
) {
    val model = hiltViewModel<EditSubscriptionModel, EditSubscriptionModelFactory> { factory ->
        factory.create(subscriptionId)
    }
    val subscription = model.subscription.collectAsStateWithLifecycle(null)
    with(model.subscriptionSettingsUseCase) {
        EditSubscriptionScreen(
            inputValid = model.inputValid,
            modelsDirty = model.modelsDirty,
            successMessage = model.successMessage,
            onDelete = model::removeSubscription,
            onSave = model::updateSubscription,
            onShare = {
                subscription.value?.let {
                    onShare(it)
                }
            },
            onExit = onExit,

            // Subscription settings repository
            supportsAuthentication = uiState.supportsAuthentication,
            url = uiState.url,
            title = uiState.title,
            titleChanged = ::setTitle,
            color = uiState.color,
            colorChanged = ::setColor,
            customUserAgent = uiState.customUserAgent,
            customUserAgentChanged = ::setCustomUserAgent,
            ignoreAlerts = uiState.ignoreAlerts,
            ignoreAlertsChanged = ::setIgnoreAlerts,
            defaultAlarmMinutes = uiState.defaultAlarmMinutes,
            defaultAlarmMinutesChanged = ::setDefaultAlarmMinutes,
            defaultAllDayAlarmMinutes = uiState.defaultAllDayAlarmMinutes,
            defaultAllDayAlarmMinutesChanged = ::setDefaultAllDayAlarmMinutes,
            ignoreDescription = uiState.ignoreDescription,
            onIgnoreDescriptionChanged = ::setIgnoreDescription,
            isCreating = false,
            requiresAuth = uiState.requiresAuth,
            username = uiState.username,
            password = uiState.password,
            onRequiresAuthChange = ::setRequiresAuth,
            onUsernameChange = ::setUsername,
            onPasswordChange = ::setPassword,
        )
    }
}

@Composable
fun EditSubscriptionScreen(
    inputValid: Boolean,
    modelsDirty: Boolean,
    successMessage: String?,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onExit: () -> Unit,

    // Subscription settings
    supportsAuthentication: Boolean,
    url: String?,
    title: String?,
    titleChanged: (String) -> Unit,
    color: Int?,
    colorChanged: (Int) -> Unit,
    customUserAgent: String?,
    customUserAgentChanged: (String) -> Unit,
    ignoreAlerts: Boolean,
    ignoreAlertsChanged: (Boolean) -> Unit,
    defaultAlarmMinutes: Long?,
    defaultAlarmMinutesChanged: (String) -> Unit,
    defaultAllDayAlarmMinutes: Long?,
    defaultAllDayAlarmMinutesChanged: (String) -> Unit,
    ignoreDescription: Boolean,
    onIgnoreDescriptionChanged: (Boolean) -> Unit,
    isCreating: Boolean,
    requiresAuth: Boolean,
    username: String? = null,
    password: String? = null,
    onRequiresAuthChange: (Boolean) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit
) {
    // show success message
    successMessage?.let {
        Toast.makeText(LocalContext.current, successMessage, Toast.LENGTH_LONG).show()
        onExit()
    }

    Scaffold(
        topBar = {
            AppBarComposable(
                valid = inputValid,
                modelsDirty = modelsDirty,
                onDelete = onDelete,
                onSave = onSave,
                onShare = onShare,
                onExit = onExit
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            SubscriptionSettingsComposable(
                modifier = Modifier.fillMaxWidth(),
                url = url,
                title = title,
                titleChanged = titleChanged,
                color = color,
                colorChanged = colorChanged,
                customUserAgent = customUserAgent,
                customUserAgentChanged = customUserAgentChanged,
                ignoreAlerts = ignoreAlerts,
                ignoreAlertsChanged = ignoreAlertsChanged,
                defaultAlarmMinutes = defaultAlarmMinutes,
                defaultAlarmMinutesChanged = defaultAlarmMinutesChanged,
                defaultAllDayAlarmMinutes = defaultAllDayAlarmMinutes,
                defaultAllDayAlarmMinutesChanged = defaultAllDayAlarmMinutesChanged,
                ignoreDescription = ignoreDescription,
                onIgnoreDescriptionChanged = onIgnoreDescriptionChanged,
                isCreating = isCreating
            )
            AnimatedVisibility(
                visible = supportsAuthentication
            ) {
                LoginCredentialsComposable(
                    requiresAuth = requiresAuth,
                    username = username,
                    password = password,
                    onRequiresAuthChange = onRequiresAuthChange,
                    onUsernameChange = onUsernameChange,
                    onPasswordChange = onPasswordChange
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBarComposable(
    valid: Boolean,
    modelsDirty: Boolean,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onExit: () -> Unit
) {
    var openDeleteDialog by remember { mutableStateOf(false) }
    if (openDeleteDialog)
        GenericAlertDialog(
            content = { Text(stringResource(R.string.edit_calendar_really_delete)) },
            confirmButton = stringResource(R.string.edit_calendar_delete) to {
                onDelete()
                openDeleteDialog = false
            },
            dismissButton = stringResource(R.string.edit_calendar_cancel) to {
                openDeleteDialog = false
            },
        ) { openDeleteDialog = false }
    var openSaveDismissDialog by remember { mutableStateOf(false) }
    if (openSaveDismissDialog) {
        GenericAlertDialog(
            content = { Text(text = if (valid)
                stringResource(R.string.edit_calendar_unsaved_changes)
            else
                stringResource(R.string.edit_calendar_need_valid_credentials)
            ) },
            confirmButton = if (valid) stringResource(R.string.edit_calendar_save) to {
                onSave()
                openSaveDismissDialog = false
            } else stringResource(R.string.edit_calendar_edit) to {
                openSaveDismissDialog = false
            },
            dismissButton = stringResource(R.string.edit_calendar_dismiss) to onExit
        ) { openSaveDismissDialog = false }
    }
    ExtendedTopAppBar(
        navigationIcon = {
            IconButton(
                onClick = {
                    if (modelsDirty)
                        openSaveDismissDialog = true
                    else
                        onExit()
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            }
        },
        title = { Text(text = stringResource(R.string.activity_edit_calendar)) },
        actions = {
            IconButton(onClick = { onShare() }) {
                Icon(
                    Icons.Filled.Share,
                    stringResource(R.string.edit_calendar_send_url)
                )
            }
            IconButton(onClick = { openDeleteDialog = true }) {
                Icon(Icons.Filled.Delete, stringResource(R.string.edit_calendar_delete))
            }
            AnimatedVisibility(visible = valid && modelsDirty) {
                IconButton(onClick = { onSave() }) {
                    Icon(Icons.Filled.Check, stringResource(R.string.edit_calendar_save))
                }
            }
        }
    )
}

@Preview
@Composable
fun EditSubscriptionScreen_Preview() {
    AppTheme {
        EditSubscriptionScreen(
            inputValid = true,
            modelsDirty = false,
            successMessage = "yay!",
            onDelete = {},
            onSave = {},
            onShare = {},
            onExit = {},
            supportsAuthentication = true,

            // Subscription settings model
            url = "url",
            title = "title",
            titleChanged = {},
            color = 0,
            colorChanged = {},
            customUserAgent = "customUserAgent",
            customUserAgentChanged = {},
            ignoreAlerts = true,
            ignoreAlertsChanged = {},
            defaultAlarmMinutes = 20L,
            defaultAlarmMinutesChanged = {},
            defaultAllDayAlarmMinutes = 10L,
            defaultAllDayAlarmMinutesChanged = {},
            ignoreDescription = false,
            onIgnoreDescriptionChanged = {},
            isCreating = true,

            // Credentials model
            requiresAuth = true,
            username = "",
            password = "",
            onRequiresAuthChange = {},
            onUsernameChange = {},
            onPasswordChange = {}
        )
    }
}