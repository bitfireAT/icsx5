/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.ui.ResourceInfo
import at.bitfire.icsdroid.ui.partials.AlertDialog

@Composable
fun EnterUrlComposable(
    requiresAuth: Boolean,
    onRequiresAuthChange: (Boolean) -> Unit,
    username: String?,
    onUsernameChange: (String) -> Unit,
    password: String?,
    onPasswordChange: (String) -> Unit,
    isInsecure: Boolean,
    url: String?,
    onUrlChange: (String) -> Unit,
    urlError: String?,
    supportsAuthentication: Boolean,
    isVerifyingUrl: Boolean,
    validationResult: ResourceInfo?,
    onValidationResultDismiss: () -> Unit,
    onPickFileRequested: () -> Unit,
    onSubmit: () -> Unit
) {
    val context = LocalContext.current

    validationResult?.exception?.let { exception ->
        val errorMessage = exception.localizedMessage ?: exception.message ?: exception.toString()
        AlertDialog(
            errorMessage, exception, onValidationResultDismiss
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isVerifyingUrl) {
        if (isVerifyingUrl) {
            snackbarHostState.showSnackbar(
                context.getString(R.string.add_calendar_validating),
                duration = SnackbarDuration.Indefinite
            )
        } else {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
        ) {
            // Instead of adding vertical padding to column, use spacer so that if content is
            // scrolled, it is not spaced
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.add_calendar_url_text),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )

            TextField(
                value = url ?: "",
                onValueChange = onUrlChange,
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions { onSubmit() },
                maxLines = 1,
                singleLine = true,
                placeholder = { Text(stringResource(R.string.add_calendar_url_sample)) },
                isError = urlError != null,
                enabled = !isVerifyingUrl
            )
            AnimatedVisibility(visible = urlError != null) {
                Text(
                    text = urlError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = stringResource(R.string.add_calendar_pick_file_text),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
            )

            TextButton(
                onClick = onPickFileRequested,
                modifier = Modifier.padding(vertical = 15.dp),
                enabled = !isVerifyingUrl
            ) {
                Text(stringResource(R.string.add_calendar_pick_file))
            }

            AnimatedVisibility(
                visible = isInsecure,
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Icon(imageVector = Icons.Rounded.Warning, contentDescription = null)

                    Text(
                        text = stringResource(R.string.add_calendar_authentication_without_https_warning),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            AnimatedVisibility(visible = supportsAuthentication) {
                LoginCredentialsComposable(
                    requiresAuth,
                    username,
                    password,
                    onRequiresAuthChange,
                    onUsernameChange,
                    onPasswordChange
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview
@Composable
fun EnterUrlComposable_Preview() {
    EnterUrlComposable(
        requiresAuth = true,
        onRequiresAuthChange = {},
        username = "previewUser",
        onUsernameChange = {},
        password = "previewUserPassword",
        onPasswordChange = {},
        isInsecure = true,
        url = "http://previewUrl.com/calendarfile.ics",
        onUrlChange = {},
        urlError = "",
        supportsAuthentication = true,
        isVerifyingUrl = true,
        validationResult = null,
        onValidationResultDismiss = {},
        onPickFileRequested = {},
        onSubmit = {}
    )
}