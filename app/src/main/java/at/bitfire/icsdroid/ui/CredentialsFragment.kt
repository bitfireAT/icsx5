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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import at.bitfire.icsdroid.R
import com.google.accompanist.themeadapter.material.MdcTheme

class CredentialsFragment: Fragment() {

    val model by activityViewModels<CredentialsModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View =
        ComposeView(requireActivity()).apply {
            setContent {
                MdcTheme {
                    LoginCredentialsComposable(
                        model.requiresAuth.observeAsState(false).value,
                        model.username.observeAsState("").value,
                        model.password.observeAsState("").value,
                        onRequiresAuthChange = { model.requiresAuth.postValue(it) },
                        onUsernameChange = { model.username.postValue(it) },
                        onPasswordChange = { model.password.postValue(it) },
                    )
                }
            }
        }

    class CredentialsModel : ViewModel() {
        var originalRequiresAuth: Boolean? = null
        var originalUsername: String? = null
        var originalPassword: String? = null

        val requiresAuth = MutableLiveData<Boolean>()
        val username = MutableLiveData<String>()
        val password = MutableLiveData<String>()

        init {
            requiresAuth.value = false
        }

        fun dirty() = requiresAuth.value != originalRequiresAuth ||
                username.value != originalUsername ||
                password.value != originalPassword
    }

}

@Composable
fun LoginCredentialsComposable(
    requiresAuth: Boolean,
    username: String,
    password: String,
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
                value = username,
                onValueChange = onUsernameChange,
                label = { Text( stringResource(R.string.add_calendar_user_name)) },
                isError = false,
                singleLine = true,
                enabled = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            PasswordTextField(
                password = password,
                labelText = stringResource(R.string.add_calendar_password),
                enabled = true,
                isError = false,
                onPasswordChange = onPasswordChange
            )
        }
    }
}

@Composable
fun PasswordTextField(
    password: String,
    labelText: String,
    enabled: Boolean,
    isError: Boolean,
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
                    Icon(painterResource(R.drawable.visibility_off), stringResource(R.string.add_calendar_password_hide))
                else
                    Icon(painterResource(R.drawable.visibility_on), stringResource(R.string.add_calendar_password_show))
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