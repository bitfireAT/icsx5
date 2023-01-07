package at.bitfire.icsdroid.ui.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.ui.model.CreateSubscriptionModel
import at.bitfire.icsdroid.ui.reusable.SwitchRow
import at.bitfire.icsdroid.ui.reusable.WarningCard

@Preview(
    showBackground = true,
)
@Composable
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
fun CreateSubscriptionLinkPage(model: CreateSubscriptionModel = viewModel()) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val url by model.url
    val urlError by model.urlError
    val showInsecureUrlWarning by model.insecureUrlWarning

    var requiresAuth by model.requiresAuth
    var username by model.username
    var password by model.password

    var showPassword by remember { mutableStateOf(false) }

    val passwordFocusRequester = FocusRequester()
    val autofillUsernameNode = AutofillNode(
        autofillTypes = listOf(AutofillType.Username, AutofillType.EmailAddress),
        onFill = { username = it },
    ).also { LocalAutofillTree.current += it }
    val autofillPasswordNode = AutofillNode(
        autofillTypes = listOf(AutofillType.Password),
        onFill = { password = it },
    ).also { LocalAutofillTree.current += it }

    AnimatedVisibility(visible = showInsecureUrlWarning) {
        WarningCard(textRes = R.string.add_calendar_authentication_without_https_warning)
    }

    OutlinedTextField(
        value = url,
        onValueChange = { model.updateUrl(it) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        label = { Text(stringResource(R.string.add_calendar_url_text)) },
        placeholder = { Text(stringResource(R.string.add_calendar_url_sample)) },
        isError = urlError != null,
        supportingText = { urlError?.let { Text(it) } },
    )

    Card {
        SwitchRow(
            title = stringResource(R.string.add_calendar_requires_authentication),
            checked = requiresAuth,
            onCheckedChanged = { requiresAuth = it },
        )
        AnimatedVisibility(visible = requiresAuth) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp),
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned {
                            autofillUsernameNode.boundingBox = it.boundsInWindow()
                        },
                    label = { Text(stringResource(R.string.add_calendar_user_name)) },
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Text,
                    ),
                    keyboardActions = KeyboardActions { keyboardController?.hide() },
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passwordFocusRequester)
                        .onGloballyPositioned {
                            autofillPasswordNode.boundingBox = it.boundsInWindow()
                        },
                    label = { Text(stringResource(R.string.add_calendar_password)) },
                    visualTransformation = if (showPassword)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Password,
                    ),
                    keyboardActions = KeyboardActions { passwordFocusRequester.requestFocus() },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword)
                                    Icons.Rounded.Visibility
                                else
                                    Icons.Rounded.VisibilityOff,
                                contentDescription = stringResource(
                                    if (showPassword)
                                        R.string.add_calendar_password_hide
                                    else
                                        R.string.add_calendar_password_show
                                ),
                            )
                        }
                    },
                )
            }
        }
    }
}
