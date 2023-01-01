package at.bitfire.icsdroid.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.ui.reusable.ColorPicker
import java.text.DateFormat
import java.util.*

@Preview(
    showBackground = true,
)
@Composable
fun SubscriptionListItem(
    subscription: Subscription = Subscription.mock,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
    ) {
        ColorPicker(color = subscription.color?.let { Color(it) }, enabled = false)
        Column(
            modifier = Modifier
                .weight(1f),
        ) {
            val syncTime = when {
                !subscription.isSynced -> stringResource(R.string.calendar_list_sync_disabled)
                subscription.lastSync == 0L -> stringResource(R.string.calendar_list_not_synced_yet)
                else -> DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT).format(Date(subscription.lastSync))
            }

            Text(
                subscription.url,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = ContentAlpha.disabled),
            )
            Text(
                subscription.displayName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                syncTime,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            val errorMessage = subscription.errorMessage
            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    errorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
