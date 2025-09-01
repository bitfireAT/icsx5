package at.bitfire.icsdroid.ui.partials

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
fun ToggleContent(
    title: String,
    description: String,
    initialToggleState: Boolean,
    onStateChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    var showTextField by rememberSaveable { mutableStateOf(initialToggleState) }
    SwitchSetting(
        title = title,
        description = description,
        checked = showTextField,
        onCheckedChange = {
            showTextField = !showTextField
            onStateChange(showTextField)
        }
    )
    AnimatedVisibility(visible = showTextField) {
        content()
    }
}