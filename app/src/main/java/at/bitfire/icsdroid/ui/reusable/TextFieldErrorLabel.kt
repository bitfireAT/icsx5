package at.bitfire.icsdroid.ui.reusable

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(bottom = 4.dp),
                color = MaterialTheme.colors.error
            )
        }
    }
}
