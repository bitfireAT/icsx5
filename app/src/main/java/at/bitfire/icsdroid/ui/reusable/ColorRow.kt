package at.bitfire.icsdroid.ui.reusable

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ColumnScope.ColorRow(
    colors: List<Color>,
    selection: Int,
    modifier: Modifier = Modifier,
    offset: Int = 0,
    onSelected: (index: Int, color: Color) -> Unit,
) {
    Row(
        modifier = modifier,
    ) {
        colors.forEachIndexed { index, color ->
            val selected = selection == (index + offset)
            ColorCircle(
                modifier = Modifier
                    .height(48.dp)
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .selectable(selected) { onSelected(index + offset, color) },
                color = color,
                strokeColor = MaterialTheme.colorScheme.error.takeIf { selected },
            )
        }
    }
}
