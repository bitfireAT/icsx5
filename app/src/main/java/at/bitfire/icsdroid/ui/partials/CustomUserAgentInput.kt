package at.bitfire.icsdroid.ui.partials

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import at.bitfire.icsdroid.Constants.COMPATIBILITY_USER_AGENTS
import at.bitfire.icsdroid.R

@Composable
fun CustomUserAgentInput(
    value: String?,
    onValueChange: (String?) -> Unit,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    ToggleContent(
        title = stringResource(R.string.add_calendar_custom_user_agent_title),
        description = stringResource(R.string.add_calendar_custom_user_agent_description),
        initialToggleState = value != null,
        onToggle = { checked -> if (!checked) onValueChange(null) },
    ) {
        DropDownTextField(
            value = value ?: "",
            onValueChange = onValueChange,
            options = COMPATIBILITY_USER_AGENTS,
            modifier = Modifier,
            label = { Text(stringResource(R.string.add_calendar_custom_user_agent_label)) },
            keyboardActions = keyboardActions
        )
    }
}