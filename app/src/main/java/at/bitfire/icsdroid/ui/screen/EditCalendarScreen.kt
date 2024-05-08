package at.bitfire.icsdroid.ui.screen

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.model.CredentialsModel
import at.bitfire.icsdroid.model.SubscriptionSettingsModel
import at.bitfire.icsdroid.ui.partials.ExtendedTopAppBar
import at.bitfire.icsdroid.ui.partials.GenericAlertDialog
import at.bitfire.icsdroid.ui.theme.AppTheme
import at.bitfire.icsdroid.ui.views.LoginCredentialsComposable
import at.bitfire.icsdroid.ui.views.SubscriptionSettingsComposable

@Composable
fun EditCalendarScreen(
    subscriptionSettingsModel: SubscriptionSettingsModel,
    credentialsModel: CredentialsModel,
    inputValid: Boolean,
    modelsDirty: Boolean,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onExit: () -> Unit
) {
    EditCalendarScreen(
        subscriptionSettingsModel,
        subscriptionSettingsModel.uiState.supportsAuthentication,

        credentialsModel,

        inputValid,
        modelsDirty,

        onDelete,
        onSave,
        onShare,
        onExit
    )
}

@Composable
private fun EditCalendarScreen(
    subscriptionSettingsModel: SubscriptionSettingsModel,
    supportsAuthentication: Boolean,

    credentialsModel: CredentialsModel,

    inputValid: Boolean,
    modelsDirty: Boolean,

    onDelete: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onExit: () -> Unit
) {
    Scaffold(
        topBar = { AppBarComposable(
            inputValid,
            modelsDirty,
            onDelete,
            onSave,
            onShare,
            onExit
        )}
    ) { paddingValues ->
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            SubscriptionSettingsComposable(
                modifier = Modifier.fillMaxWidth(),
                subscriptionSettingsModel,
                isCreating = false
            )
            AnimatedVisibility(visible = supportsAuthentication) {
                LoginCredentialsComposable(
                    credentialsModel
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
fun EditCalendarScreen_Preview() {
    AppTheme {
        EditCalendarScreen(
            subscriptionSettingsModel = viewModel(),
            supportsAuthentication = true,

            credentialsModel = viewModel(),

            inputValid = true,
            modelsDirty = true,

            onDelete = {},
            onSave = {},
            onShare = {},
            onExit = {}
        )
    }
}