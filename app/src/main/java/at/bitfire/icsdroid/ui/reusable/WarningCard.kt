package at.bitfire.icsdroid.ui.reusable

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.bitfire.icsdroid.R

@Composable
fun WarningCard(@StringRes textRes: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = stringResource(R.string.warning),
                modifier = Modifier
                    .size(56.dp)
                    .padding(8.dp),
            )
            Text(
                text = stringResource(textRes),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .padding(end = 8.dp),
            )
        }
    }
}

@Preview
@Composable
fun WarningCardPreview() {
    WarningCard(textRes = R.string.add_calendar_authentication_without_https_warning)
}
