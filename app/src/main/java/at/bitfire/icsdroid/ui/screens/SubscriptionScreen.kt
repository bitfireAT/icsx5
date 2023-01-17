package at.bitfire.icsdroid.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.CalendarCredentials
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.ui.activity.MainActivity.Companion.Paths
import at.bitfire.icsdroid.ui.dialog.AlarmSetDialog
import at.bitfire.icsdroid.ui.reusable.ColorPicker
import at.bitfire.icsdroid.ui.reusable.SwitchRow

@Composable
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
fun SubscriptionScreen(navHostController: NavHostController, subscription: Subscription) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    val (usernameCred, passwordCred) = remember { CalendarCredentials(context).get(subscription) }

    var displayName by remember { mutableStateOf(subscription.displayName) }
    var color by remember { mutableStateOf(subscription.color) }
    var ignoreEmbeddedAlerts by remember { mutableStateOf(subscription.ignoreEmbeddedAlerts) }
    var defaultAlarmMinutes by remember { mutableStateOf(subscription.defaultAlarmMinutes) }
    var syncEvents by remember { mutableStateOf(subscription.syncEvents) }
    var needsAuthorization by remember { mutableStateOf(usernameCred != null && passwordCred != null) }
    var username by remember { mutableStateOf(usernameCred ?: "") }
    var password by remember { mutableStateOf(passwordCred ?: "") }

    var showDefaultAlarmPicker by remember { mutableStateOf(false) }

    var dirty by remember { mutableStateOf(false) }

    fun checkDirty() {
        dirty = listOf(
            displayName to subscription.displayName,
            color to subscription.color,
            ignoreEmbeddedAlerts to subscription.ignoreEmbeddedAlerts,
            defaultAlarmMinutes to subscription.defaultAlarmMinutes,
            needsAuthorization to (usernameCred != null && passwordCred != null),
            username to usernameCred,
            password to passwordCred,
        ).any { (a, b) -> a != b }
    }

    fun onBack() {
        Paths.Subscriptions.navigate(navHostController)
    }

    BackHandler(onBack = ::onBack)

    if (showDefaultAlarmPicker)
        AlarmSetDialog(
            { showDefaultAlarmPicker = false },
            { newMinutes ->
                defaultAlarmMinutes = newMinutes
                showDefaultAlarmPicker = false
                checkDirty()
            }
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.activity_edit_calendar)) },
                navigationIcon = {
                    IconButton(onClick = ::onBack) {
                        Icon(
                            Icons.Rounded.KeyboardArrowLeft,
                            stringResource(R.string.edit_calendar_cancel),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { TODO() }) {
                        Icon(
                            Icons.Rounded.Share,
                            stringResource(R.string.edit_calendar_send_url),
                        )
                    }
                    IconButton(onClick = { TODO() }) {
                        Icon(
                            Icons.Rounded.Delete,
                            stringResource(R.string.edit_calendar_delete),
                        )
                    }
                    AnimatedVisibility(visible = dirty) {
                        IconButton(onClick = { TODO() }) {
                            Icon(
                                Icons.Rounded.Save,
                                stringResource(R.string.edit_calendar_save),
                            )
                        }
                    }
                }
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp),
        ) {
            Text(
                text = subscription.url.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = ContentAlpha.disabled),
            )
            Text(
                text = stringResource(R.string.add_calendar_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            TextField(
                value = displayName,
                onValueChange = { displayName = it; checkDirty() },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    autoCorrect = true,
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text,
                ),
                keyboardActions = KeyboardActions { keyboardController?.hide() },
                singleLine = true,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    ColorPicker(
                        color = color?.let { Color(it) },
                        modifier = Modifier
                            .padding(end = 20.dp),
                    ) { newColor ->
                        color = newColor.toArgb()
                        checkDirty()
                    }
                },
            )

            Text(
                text = stringResource(R.string.add_calendar_alarms_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            SwitchRow(
                title = stringResource(R.string.add_calendar_alarms_ignore_title),
                subtitle = stringResource(R.string.add_calendar_alarms_ignore_description),
                checked = ignoreEmbeddedAlerts,
                onCheckedChanged = { ignoreEmbeddedAlerts = it; checkDirty() },
            )
            SwitchRow(
                title = stringResource(R.string.add_calendar_alarms_default_title),
                checked = defaultAlarmMinutes != null,
                onCheckedChanged = { checked ->
                    if (!checked)
                        defaultAlarmMinutes = null
                    else {
                        showDefaultAlarmPicker = true
                    }
                },
            )

            Text(
                text = stringResource(R.string.edit_calendar_options),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            SwitchRow(
                title = stringResource(R.string.edit_calendar_sync_this_calendar),
                checked = syncEvents,
                onCheckedChanged = { syncEvents = it; checkDirty() },
            )

            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.add_calendar_requires_authentication),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = needsAuthorization,
                    onCheckedChange = { needsAuthorization = it },
                )
            }
            AnimatedVisibility(visible = needsAuthorization) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val passwordFocusRequester = FocusRequester()
                    var showPassword by remember { mutableStateOf(false) }

                    val autofillUsernameNode = AutofillNode(
                        autofillTypes = listOf(AutofillType.Username, AutofillType.EmailAddress),
                        onFill = { username = it },
                    ).also { LocalAutofillTree.current += it }
                    val autofillPasswordNode = AutofillNode(
                        autofillTypes = listOf(AutofillType.Password),
                        onFill = { password = it },
                    ).also { LocalAutofillTree.current += it }

                    TextField(
                        value = username,
                        onValueChange = { username = it; checkDirty() },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Text,
                        ),
                        keyboardActions = KeyboardActions { passwordFocusRequester.requestFocus() },
                        singleLine = true,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned {
                                autofillUsernameNode.boundingBox = it.boundsInWindow()
                            },
                    )
                    TextField(
                        value = password,
                        onValueChange = { password = it; checkDirty() },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Password,
                        ),
                        keyboardActions = KeyboardActions { keyboardController?.hide() },
                        visualTransformation = if (showPassword)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        singleLine = true,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocusRequester)
                            .onGloballyPositioned {
                                autofillPasswordNode.boundingBox = it.boundsInWindow()
                            },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword)
                                        Icons.Rounded.VisibilityOff
                                    else
                                        Icons.Rounded.Visibility,
                                    stringResource(
                                        if (showPassword)
                                            R.string.add_calendar_password_hide
                                        else
                                            R.string.add_calendar_password_show,
                                    )
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}
