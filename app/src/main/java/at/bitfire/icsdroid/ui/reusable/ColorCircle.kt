package at.bitfire.icsdroid.ui.reusable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ColorCircle(
    color: Color,
    size: Dp,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val selectionColor = contentColorFor(color)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = color,
            shape = CircleShape,
            border = BorderStroke(size.times(.05f), selectionColor).takeIf { isSelected },
            modifier = Modifier.size(size)
        ) {
            // No need to have any contents
        }
    }
}

@Preview
@Composable
fun ColorCircle_PreviewSelected() {
    ColorCircle(color = Color.Blue, size = 48.dp, isSelected = true)
}

@Preview
@Composable
fun ColorCircle_PreviewNotSelected() {
    ColorCircle(color = Color.Blue, size = 48.dp, isSelected = false)
}

@Preview
@Composable
fun ColorCircle_PreviewResized() {
    ColorCircle(
        color = Color.Blue,
        size = 48.dp,
        isSelected = true,
        modifier = Modifier.size(96.dp)
    )
}
