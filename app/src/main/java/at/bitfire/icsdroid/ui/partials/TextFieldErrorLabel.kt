package at.bitfire.icsdroid.ui.partials

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TextFieldErrorLabel(error: String?) {
    AnimatedContent(
        targetState = error,
        label = "show/hide error"
    ) { err ->
        err?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 4.dp),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
