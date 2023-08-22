package at.bitfire.icsdroid.ui.reusable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp

@Composable
fun SwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
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
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .clickable(enabled) { onCheckedChange(!checked) }
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.subtitle2,
                modifier = Modifier.fillMaxWidth()
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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
