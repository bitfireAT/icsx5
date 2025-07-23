/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.bitfire.icsdroid.R

@Composable
fun LoginCredentialsComposable(
    requiresAuth: Boolean,
    username: String? = null,
    password: String? = null,
    onRequiresAuthChange: (Boolean) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit
) {
    val usernameError = if (username?.isBlank() == true)
        stringResource(R.string.edit_calendar_need_username)
    else null
    val passwordError = if (username?.isBlank() == true)
        stringResource(R.string.edit_calendar_need_password)
    else null
    Column(
        Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.add_calendar_authentication_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.add_calendar_requires_authentication),
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = requiresAuth,
                onCheckedChange = onRequiresAuthChange,
            )
        }
        if (requiresAuth) {
            OutlinedTextField(
                value = username ?: "",
                onValueChange = onUsernameChange,
                label = { Text(stringResource(R.string.add_calendar_user_name)) },
                supportingText = { Text(usernameError ?: stringResource(R.string.required_annotation)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = usernameError != null,
                modifier = Modifier.fillMaxWidth()
            )

            PasswordTextField(
                password = password ?: "",
                labelText = stringResource(R.string.add_calendar_password),
                supportingText = passwordError ?: stringResource(R.string.required_annotation),
                isError = passwordError != null,
                errorText = passwordError,
                onPasswordChange = onPasswordChange
            )
        }
    }
}

@Composable
fun PasswordTextField(
    password: String,
    labelText: String = "",
    supportingText: String = "",
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
    onPasswordChange: (String) -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(labelText) },
        supportingText = { Text(errorText ?: supportingText) },
        isError = isError,
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                if (passwordVisible)
                    Icon(Icons.Rounded.VisibilityOff, stringResource(R.string.add_calendar_password_hide))
                else
                    Icon(Icons.Rounded.Visibility, stringResource(R.string.add_calendar_password_show))
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
@Preview
fun LoginCredentialsComposable_Preview() {
    LoginCredentialsComposable(
        requiresAuth = true,
        username = "Demo user",
        password = "demo password",
        onRequiresAuthChange = {},
        onUsernameChange = {},
        onPasswordChange = {}
    )
}

@Composable
@Preview
fun LoginCredentialsComposable_Preview_Empty() {
    LoginCredentialsComposable(
        requiresAuth = true,
        username = null,
        password = null,
        onRequiresAuthChange = {},
        onUsernameChange = {},
        onPasswordChange = {}
    )
}

@Composable
@Preview
fun LoginCredentialsComposable_Preview_Error() {
    LoginCredentialsComposable(
        requiresAuth = true,
        username = "",
        password = "",
        onRequiresAuthChange = {},
        onUsernameChange = {},
        onPasswordChange = {}
    )
}