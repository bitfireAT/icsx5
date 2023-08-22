package at.bitfire.icsdroid.ui.subscription

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.ui.modifier.autofill
import at.bitfire.icsdroid.ui.reusable.SwitchRow

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun ColumnScope.SubscriptionCredentials(model: SubscriptionCredentialsModel = viewModel()) {
    val softwareKeyboard = LocalSoftwareKeyboardController.current

    val requiresAuth by model.requiresAuth.observeAsState(initial = false)
    val username by model.username.observeAsState("")
    val password by model.password.observeAsState("")

    SwitchRow(
        checked = requiresAuth,
        onCheckedChange = { model.requiresAuth.value = it },
        text = stringResource(R.string.add_calendar_requires_authentication)
    )

    val passwordFieldFocusRequester = remember { FocusRequester() }

    AnimatedVisibility(visible = requiresAuth) {
        TextField(
            value = username,
            onValueChange = { model.username.value = it },
            label = { Text(stringResource(R.string.add_calendar_user_name)) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrect = true,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions { passwordFieldFocusRequester.requestFocus() },
            singleLine = true,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .autofill(listOf(AutofillType.Username)) { model.username.value = it }
        )
        TextField(
            value = password,
            onValueChange = { model.password.value = it },
            label = { Text(stringResource(R.string.add_calendar_password)) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrect = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions { softwareKeyboard?.hide() },
            singleLine = true,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .autofill(listOf(AutofillType.Password)) { model.password.value = it }
        )
    }
}


@Preview(showSystemUi = true, showBackground = true)
@Composable
fun SubscriptionCredentials_Preview() {
    Column {
        SubscriptionCredentials()
    }
}
