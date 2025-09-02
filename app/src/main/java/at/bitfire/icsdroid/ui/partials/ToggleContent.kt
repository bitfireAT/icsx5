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
    onToggle: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    var showContent by rememberSaveable { mutableStateOf(initialToggleState) }
    SwitchSetting(
        title = title,
        description = description,
        checked = showContent,
        onCheckedChange = {
            showContent = !showContent
            onToggle(showContent)
        }
    )
    AnimatedVisibility(visible = showContent) {
        content()
    }
}