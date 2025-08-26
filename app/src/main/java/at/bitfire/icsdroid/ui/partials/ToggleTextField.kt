package at.bitfire.icsdroid.ui.partials

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import at.bitfire.icsdroid.R

@Composable
fun ToggleTextField(
    title: String,
    description: String,
    onValueChange: (String) -> Unit,
    value: String?,
    keyboardActions: KeyboardActions
) {
    var showTextField by rememberSaveable { mutableStateOf(false) }
    SwitchSetting(
        title = title,
        description = description,
        checked = showTextField,
        onCheckedChange = {
            showTextField = !showTextField
            if (!showTextField)
                onValueChange("")
        }
    )
    AnimatedVisibility(visible = showTextField) {
        OutlinedTextField(
            value = value ?: "",
            onValueChange = onValueChange,
            label = { Text(stringResource(R.string.add_calendar_custom_user_agent_label)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardActions = keyboardActions
        )
    }
}