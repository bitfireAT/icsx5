/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.ui.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.ui.ResourceInfo
import at.bitfire.icsdroid.ui.partials.AlertDialog
import at.bitfire.icsdroid.ui.partials.CustomUserAgentInput
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
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
    customUserAgent: String?,
    onCustomUserAgentChange: (String?) -> Unit,
    fileName: String?,
    onUrlChange: (String?) -> Unit,
    urlError: String?,
    acceptedProtocol: Boolean,
    isVerifyingUrl: Boolean,
    validationResult: ResourceInfo?,
    onValidationResultDismiss: () -> Unit,
    onPickFileRequested: () -> Unit,
    onSubmit: () -> Unit
) {
    validationResult?.exception?.let { exception ->
        val errorMessage = exception.localizedMessage ?: exception.message ?: exception.toString()
        AlertDialog(
            errorMessage, exception, onValidationResultDismiss
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val addCalendarValidatingString = stringResource(R.string.add_calendar_validating)
    LaunchedEffect(isVerifyingUrl) {
        if (isVerifyingUrl) {
            snackbarHostState.showSnackbar(
                addCalendarValidatingString,
                duration = SnackbarDuration.Indefinite
            )
        } else {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // The WindowInsets are already applied on the Scaffold of AddCalendarScreen
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
        ) {
            AnimatedVisibility(isVerifyingUrl) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            val scope = rememberCoroutineScope()
            val state = rememberPagerState(pageCount = { 2 })

            TabRow(state.currentPage) {
                Tab(state.currentPage == 0, onClick = {
                    onUrlChange(null)
                    scope.launch { state.scrollToPage(0) }},
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    val color = if (state.currentPage == 0) MaterialTheme.colorScheme.primary else Color.Gray
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        Icons.Default.Link,
                        stringResource(R.string.add_calendar_subscribe_url),
                        tint = color
                    )
                    Text(
                        stringResource(R.string.add_calendar_subscribe_url).uppercase(),
                        modifier = Modifier.padding(8.dp, 0.dp, 8.dp, 4.dp),
                        color = color,
                        fontSize = 3.em,
                        fontWeight = FontWeight.Bold
                    )
                }
                Tab(state.currentPage == 1, onClick = {
                    onUrlChange(null)
                    scope.launch { state.scrollToPage(1) }},
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    val color = if (state.currentPage == 1) MaterialTheme.colorScheme.primary else Color.Gray
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        Icons.Default.FolderOpen,
                        stringResource(R.string.add_calendar_subscribe_url),
                        tint = color
                    )
                    Text(
                        stringResource(R.string.add_calendar_subscribe_file).uppercase(),
                        modifier = Modifier.padding(8.dp, 0.dp, 8.dp, 4.dp),
                        color = color,
                        fontSize = 3.em,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Instead of adding vertical padding to column, use spacer so that if content is
            // scrolled, it is not spaced
            Spacer(modifier = Modifier.height(16.dp))

            HorizontalPager(
                state,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .weight(1f),
                verticalAlignment = Alignment.Top
            ) { index ->
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())) {
                    when (index) {
                        0 -> SubscribeToUrl(
                            url,
                            onUrlChange,
                            customUserAgent,
                            onCustomUserAgentChange,
                            onSubmit,
                            urlError,
                            isVerifyingUrl,
                            isInsecure,
                            acceptedProtocol,
                            requiresAuth,
                            username,
                            password,
                            onRequiresAuthChange,
                            onUsernameChange,
                            onPasswordChange
                        )

                        1 -> SubscribeToFile(
                            fileName,
                            onUrlChange,
                            onSubmit,
                            urlError,
                            isVerifyingUrl,
                            onPickFileRequested
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ColumnScope.SubscribeToUrl(
    url: String?,
    onUrlChange: (String) -> Unit,
    customUserAgent: String?,
    onCustomUserAgentChange: (String?) -> Unit,
    onSubmit: () -> Unit,
    error: String?,
    verifying: Boolean,
    isInsecure: Boolean,
    validUrlInput: Boolean,
    requiresAuth: Boolean,
    username: String?,
    password: String?,
    onRequiresAuthChange: (Boolean) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit
) {

    // URL
    ResourceInput(
        url,
        onUrlChange,
        verifying,
        onSubmit,
        error,
        labelText = stringResource(R.string.add_calendar_pick_url_label)
    )
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

    // Optional settings
    AnimatedVisibility(visible = validUrlInput) {
        Column {
            // Username + Password
            LoginCredentialsComposable(
                requiresAuth,
                username,
                password,
                onRequiresAuthChange,
                onUsernameChange,
                onPasswordChange
            )

            Spacer(modifier = Modifier.padding(12.dp))

            // Advanced
            Text(
                text = stringResource(R.string.add_calendar_advanced_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Custom User Agent
            CustomUserAgentInput(
                value = customUserAgent,
                onValueChange = onCustomUserAgentChange,
                keyboardActions = KeyboardActions { onSubmit() }
            )
        }
    }

}

@Composable
private fun ColumnScope.SubscribeToFile(
    filename: String?,
    onChange: (String) -> Unit,
    onSubmit: () -> Unit,
    error: String?,
    verifying: Boolean,
    onPickFileRequested: () -> Unit
) {
    ResourceInput(
        filename,
        onChange,
        verifying,
        onSubmit,
        error,
        stringResource(R.string.add_calendar_pick_file),
        readOnly = true,
        onPickFileRequested
    )
}

@Composable
private fun ColumnScope.ResourceInput(
    value: String?,
    onChange: (String) -> Unit,
    disabled: Boolean,
    onSubmit: () -> Unit,
    error: String?,
    labelText: String,
    readOnly: Boolean = false,
    onClick: () -> Unit = {}
) {
    TextField(
        value = value ?: "",
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 16.dp),
        enabled = !disabled,
        readOnly = readOnly,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Go
        ),
        keyboardActions = KeyboardActions { onSubmit() },
        maxLines = 8,
        label = { Text(labelText) },
        isError = error != null,
        interactionSource = remember { MutableInteractionSource() }.also { interactionSource ->
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect {
                    if (it is PressInteraction.Release)
                        onClick()
                }
            }
        }
    )
    AnimatedVisibility(visible = error != null) {
        Text(
            text = error ?: "",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall
        )
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
        url = "http://previewUrl.com/looong/looong/looong/looong/looong/looong/calendarfile.ics" +
            "\n\n a\n b\n c\n\n" +
            "http://previewUrl.com/looong/looong/looong/looong/looong/looong/calendarfile.ics",
        customUserAgent = "previewUserAgent",
        onCustomUserAgentChange = {},
        fileName = "file name",
        onUrlChange = {},
        urlError = "",
        acceptedProtocol = true,
        isVerifyingUrl = true,
        validationResult = null,
        onValidationResultDismiss = {},
        onPickFileRequested = {},
        onSubmit = {}
    )
}