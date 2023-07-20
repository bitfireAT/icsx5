package at.bitfire.icsdroid.ui.list

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.ui.reusable.ColorCircle
import java.text.DateFormat
import java.util.Date

@Composable
fun CalendarListItem(
    subscription: Subscription,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .clickable(onClick = onClick)
            .then(modifier)
    ) {
        ColorCircle(
            color = subscription.color?.let { Color(it) } ?: Color.Black,
            size = 40.dp,
            modifier = Modifier.padding(end = 20.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = subscription.url.toString(),
                style = MaterialTheme.typography.caption,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colors.onBackground.copy(ContentAlpha.medium)
            )
            Text(
                text = subscription.displayName,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = subscription.lastSync?.let { lastSync ->
                    DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT)
                        .format(Date(lastSync))
                } ?: stringResource(R.string.calendar_list_not_synced_yet),
                style = MaterialTheme.typography.body2,
                modifier = Modifier.fillMaxWidth()
            )
            subscription.errorMessage?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.fillMaxWidth(),
                    color = colorResource(R.color.redorange)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarListItem_Preview(
    @PreviewParameter(CalendarListItemPreviewProvider::class) data: CalendarListItemPreviewData
) {
    CalendarListItem(
        Subscription(
            displayName = data.displayName,
            url = data.url,
            color = data.color,
            lastSync = data.lastUpdate,
            errorMessage = data.errorMessage
        )
    ) { }
}

data class CalendarListItemPreviewData(
    val displayName: String,
    val url: Uri,
    val color: Int? = null,
    val lastUpdate: Long? = null,
    val errorMessage: String? = null
)

class CalendarListItemPreviewProvider : PreviewParameterProvider<CalendarListItemPreviewData> {
    override val values: Sequence<CalendarListItemPreviewData> = sequenceOf(
        // Subscription with color, no error, no last update
        CalendarListItemPreviewData(
            url = Uri.parse("http://example.com/mycalendar.ics"),
            displayName = "Example Subscription",
            color = Color.Red.toArgb()
        ),
        // Subscription with color, no error, with last update
        CalendarListItemPreviewData(
            url = Uri.parse("http://example.com/mycalendar.ics"),
            displayName = "Example Subscription",
            color = Color.Red.toArgb(),
            lastUpdate = System.currentTimeMillis()
        ),
        // Subscription without color, with error, no last update
        CalendarListItemPreviewData(
            url = Uri.parse("http://example.com/mycalendar.ics"),
            displayName = "Example Subscription",
            errorMessage = "Testing error message.\nThis can be multiline"
        )
    )
}
