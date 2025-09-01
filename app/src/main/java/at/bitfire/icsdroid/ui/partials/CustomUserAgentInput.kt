package at.bitfire.icsdroid.ui.partials

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import at.bitfire.icsdroid.R

@Composable
fun CustomUserAgentInput(
    onValueChange: (String?) -> Unit,
    value: String?,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    ToggleContent(
        title = stringResource(R.string.add_calendar_custom_user_agent_title),
        description = stringResource(R.string.add_calendar_custom_user_agent_description),
        initialToggleState = value != null,
        onStateChange = { checked -> if (!checked) onValueChange(null) },
    ) {
        OutlinedTextField(
            value = value ?: "",
            onValueChange = onValueChange,
            label = { Text(stringResource(R.string.add_calendar_custom_user_agent_label)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardActions = keyboardActions
        )
    }
}