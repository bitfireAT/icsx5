package at.bitfire.icsdroid.ui.reusable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
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
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = color,
            shape = CircleShape,
            modifier = Modifier.size(size)
        ) {
            // No need to have any contents
        }
    }
}

@Preview
@Composable
fun ColorCircle_Preview() {
    ColorCircle(color = Color.Blue, size = 48.dp)
}

@Preview
@Composable
fun ColorCircle_PreviewResized() {
    ColorCircle(
        color = Color.Blue,
        size = 48.dp,
        modifier = Modifier.size(96.dp)
    )
}
