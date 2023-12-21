/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.model.CredentialsModel

class CredentialsFragment: Fragment() {

    val model by activityViewModels<CredentialsModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View =
        ComposeView(requireActivity()).apply {
            setContent {
                val requiresAuth by model.requiresAuth.observeAsState(false)
                val username by model.username.observeAsState()
                val password by model.password.observeAsState()

                LoginCredentialsComposable(
                    requiresAuth,
                    username,
                    password,
                    onRequiresAuthChange = { model.requiresAuth.postValue(it) },
                    onUsernameChange = { model.username.postValue(it) },
                    onPasswordChange = { model.password.postValue(it) },
                )
            }
        }

}

@Composable
fun LoginCredentialsComposable(
    requiresAuth: Boolean,
    username: String?,
    password: String?,
    onRequiresAuthChange: (Boolean) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit
) {
    Column(
        Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.add_calendar_requires_authentication),
                style = MaterialTheme.typography.body1,
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
                label = { Text( stringResource(R.string.add_calendar_user_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            PasswordTextField(
                password = password ?: "",
                labelText = stringResource(R.string.add_calendar_password),
                onPasswordChange = onPasswordChange
            )
        }
    }
}

@Composable
fun PasswordTextField(
    password: String,
    labelText: String = "",
    enabled: Boolean = true,
    isError: Boolean = false,
    onPasswordChange: (String) -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(labelText) },
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
        password = "",
        onRequiresAuthChange = {},
        onUsernameChange = {},
        onPasswordChange = {}
    )
}