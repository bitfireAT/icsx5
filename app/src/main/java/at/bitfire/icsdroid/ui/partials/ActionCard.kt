package at.bitfire.icsdroid.ui.partials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Provides a card with a title, a description, and a button for performing an action.
 */
@Composable
fun ActionCard(
    title: String,
    message: String?,
    actionText: String,
    modifier: Modifier = Modifier,
    onAction: () -> Unit
) {
    ElevatedCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(
                    onClick = onAction
                ) {
                    Text(text = actionText.uppercase())
                }
            }
            if (message != null) {
                Text(
                    text = message,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ActionCard_Preview() {
    ActionCard(
        title = "Testing Card",
        message = "This is the message shown in the card. Can be pretty long",
        actionText = "Action",
        onAction = {}
    )
}
