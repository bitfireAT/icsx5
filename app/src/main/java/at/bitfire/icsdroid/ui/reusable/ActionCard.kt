package at.bitfire.icsdroid.ui.reusable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
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
    message: String,
    actionText: String,
    modifier: Modifier = Modifier,
    onAction: () -> Unit
) {
    Card(
        modifier = modifier,
        elevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.h6
            )
            Text(
                text = message,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.body2
            )
            TextButton(
                onClick = onAction,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = actionText)
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
