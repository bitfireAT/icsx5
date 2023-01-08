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
import at.bitfire.icsdroid.ui.reusable.RequiresAuthCard
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
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions { keyboardController?.hide() },
    )

    RequiresAuthCard(
        requiresAuthState = model.requiresAuth,
        usernameState = model.username,
        passwordState = model.password,
    )
}
