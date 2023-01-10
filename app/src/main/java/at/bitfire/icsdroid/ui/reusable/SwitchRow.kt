package at.bitfire.icsdroid.ui.reusable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SwitchRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChanged: (checked: Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            subtitle?.let {
                Text(
                    text = it,
                    modifier = Modifier
                        .fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChanged,
            modifier = Modifier
                .padding(12.dp),
            enabled = enabled,
        )
    }
}

@Preview(
    showBackground = true
)
@Composable
fun SwitchRowPreview() {
    SwitchRow(title = "Demo", subtitle = "Subtitle", checked = true, onCheckedChanged = {})
}
