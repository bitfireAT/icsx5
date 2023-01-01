package at.bitfire.icsdroid.ui.reusable

import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.material.color.MaterialColors

/**
 * Draws a circle, and optionally, a stroke around it.
 * @author Arnau Mora
 * @since 20220101
 * @param modifier The modifier to apply to the view. Size, align...
 * @param color The color of the circle.
 * @param strokeWidth The width of the stroke if any.
 * @param strokeColor If not null, the color of the stroke to draw.
 */
@Composable
fun ColorCircle(
    modifier: Modifier = Modifier,
    color: Color,
    strokeWidth: Dp = 4.dp,
    strokeColor: Color? = null,
) {
    val check = rememberVectorPainter(image = Icons.Rounded.Check)
    Canvas(
        modifier = modifier,
    ) {
        drawCircle(color = color)
        if (strokeColor != null) {
            val stroke = Color(
                MaterialColors.harmonize(strokeColor.toArgb(), color.toArgb())
            )
            drawCircle(
                color = stroke,
                style = Stroke(
                    width = strokeWidth.toPx(),
                ),
            )
            with(check) {
                translate(size.width / 2 - intrinsicSize.width / 2, size.height / 2 - intrinsicSize.height / 2) {
                    draw(intrinsicSize, colorFilter = ColorFilter.tint(stroke))
                }
            }
        }
    }
}
