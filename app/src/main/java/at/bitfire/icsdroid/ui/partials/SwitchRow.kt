package at.bitfire.icsdroid.ui.partials

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider

@Composable
fun SwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
        Text(
            text = text,
            style = MaterialTheme.typography.caption,
            modifier = Modifier
                .weight(1f)
                .clickable(enabled) { onCheckedChange(!checked) }
        )
    }
}

class SwitchRowPreview: PreviewParameterProvider<SwitchRowPreview.SwitchRowPreviewData> {
    data class SwitchRowPreviewData(
        val checked: Boolean,
        val enabled: Boolean
        ) {
        val text = "SwitchRow ${if (checked) "checked" else "unchecked"} ${if (enabled) "enabled" else "disabled"}"
    }

    override val values: Sequence<SwitchRowPreviewData> = sequenceOf(
        SwitchRowPreviewData(checked = true, enabled = true),
        SwitchRowPreviewData(checked = false, enabled = true),
        SwitchRowPreviewData(checked = true, enabled = false),
        SwitchRowPreviewData(checked = false, enabled = false),
    )
}

@Preview
@Composable
fun SwitchRow_PreviewChecked(
    @PreviewParameter(SwitchRowPreview::class) state: SwitchRowPreview.SwitchRowPreviewData
) {
    SwitchRow(
        checked = state.checked,
        enabled = state.enabled,
        onCheckedChange = {},
        text = state.text
    )
}
