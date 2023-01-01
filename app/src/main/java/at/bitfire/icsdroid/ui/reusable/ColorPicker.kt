package at.bitfire.icsdroid.ui.reusable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import at.bitfire.icsdroid.R

@Composable
fun ColorPicker(
    color: Color?,
    enabled: Boolean = true,
    onColorPicked: (color: Color) -> Unit = {}
) {
    var currentColor by remember { mutableStateOf(color ?: Color.White) }
    var currentColorIndex by remember { mutableStateOf(0) }
    var showingDialog by remember { mutableStateOf(false) }

    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        Color(0xFFF44336), // RED 500
        Color(0xFFE91E63), // PINK 500
        Color(0xFFFF2C93), // LIGHT PINK 500
        Color(0xFF9C27B0), // PURPLE 500
        Color(0xFF673AB7), // DEEP PURPLE 500
        Color(0xFF3F51B5), // INDIGO 500
        Color(0xFF2196F3), // BLUE 500
        Color(0xFF03A9F4), // LIGHT BLUE 500
        Color(0xFF00BCD4), // CYAN 500
        Color(0xFF009688), // TEAL 500
        Color(0xFF4CAF50), // GREEN 500
        Color(0xFF8BC34A), // LIGHT GREEN 500
        Color(0xFFCDDC39), // LIME 500
        Color(0xFFFFEB3B), // YELLOW 500
        Color(0xFFFFC107), // AMBER 500
        Color(0xFFFF9800), // ORANGE 500
        Color(0xFF795548), // BROWN 500
        Color(0xFF607D8B), // BLUE GREY 500
        Color(0xFF9E9E9E), // GREY 500
    )

    if (showingDialog)
        AlertDialog(
            onDismissRequest = { showingDialog = false },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.color_dialog_dynamic),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                    )
                    ColorRow(
                        colors = colors.subList(0, 3),
                        selection = currentColorIndex,
                        offset = 0,
                        modifier = Modifier.fillMaxWidth(),
                    ) { i, c -> currentColorIndex = i; currentColor = c }

                    Text(
                        text = stringResource(R.string.color_dialog_material),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp, top = 8.dp),
                    )
                    colors
                        .subList(3, colors.size)
                        .mapIndexed { index, color -> index to color }
                        // Create groups of 5 colors
                        .groupBy { (i, _) -> i % 5 }
                        .map { group -> group.value.map { it.second } }
                        .forEachIndexed { index, colors ->
                            ColorRow(
                                colors = colors,
                                selection = currentColorIndex,
                                offset = 3 + 4 * index,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            ) { i, c -> currentColorIndex = i; currentColor = c }
                        }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showingDialog = false
                    onColorPicked(currentColor)
                }) {
                    Text(stringResource(R.string.color_dialog_select))
                }
            },
            dismissButton = {
                TextButton(onClick = { /*TODO*/ }) {
                    Text(stringResource(R.string.color_dialog_custom))
                }
            }
        )

    ColorCircle(
        modifier = Modifier
            .size(52.dp)
            .padding(end = 20.dp)
            .clickable(enabled) { showingDialog = true },
        color = color ?: Color.White,
    )
}