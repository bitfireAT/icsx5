package at.bitfire.icsdroid.ui.reusable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import at.bitfire.icsdroid.R
import kotlin.math.ceil

private val legacyColorPalette = arrayOf(
    Color(0xFFE57373), // Red 300
    Color(0xFFF06292), // Pink 300
    Color(0xFFBA68C8), // Purple 300
    Color(0xFF9575CD), // Deep Purple 300
    Color(0xFF7986CB), // Indigo 300
    Color(0xFF64B5F6), // Blue 300

    Color(0xFF4FC3F7), // Light Blue 300
    Color(0xFF4DD0E1), // Cyan 300
    Color(0xFF4DB6AC), // Teal 300
    Color(0xFF81C784), // Green 300
    Color(0xFFAED581), // Light Green 300
    Color(0xFFDCE775), // Lime 300

    Color(0xFFFFF176), // Yellow 300
    Color(0xFFFFD54F), // Amber 300
    Color(0xFFFFB74D), // Orange 300
    Color(0xFFFF8A65), // Deep Orange 300
    Color(0xFFA1887F), // Brown 300
    Color(0xFFE0E0E0), // Gray 300

    Color(0xFF90A4AE), // Blue Gray 300
    Color(0xFF000000), // Black
    Color(0xFFFFFFFF), // White
)

/**
 * The size in Dp of every circle in the color picker.
 */
private const val ColorPickerCircleSize = 36

/**
 * The amount of colors in each row of the picker.
 */
private const val ColorsPerRow = 6

@Composable
fun ColorPickingLayout(selectedColor: Color?, onColorSelected: (Color) -> Unit) {
    var colorHex: String by remember {
        mutableStateOf(selectedColor?.toArgb()?.toString(16)?.uppercase() ?: "")
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        val rows = ceil(legacyColorPalette.size / ColorsPerRow.toFloat()).toInt()
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until ColorsPerRow) {
                    val offset = row * ColorsPerRow
                    val color = legacyColorPalette.getOrNull(offset + col) ?: break

                    ColorCircle(
                        color,
                        ColorPickerCircleSize.dp,
                        selectedColor == color,
                        Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .clickable {
                                onColorSelected(color)
                            }
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            ColorCircle(
                color = selectedColor ?: Color.Unspecified,
                size = 24.dp,
                isSelected = false
            )
            OutlinedTextField(
                value = colorHex,
                onValueChange = {
                    colorHex = it.uppercase()
                    try {
                        val color = colorHex.toInt(16)
                        onColorSelected(Color(color))
                    } catch (ignored: NumberFormatException) { }
                },
                label = { Text(stringResource(R.string.color_picker_field)) },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                leadingIcon = {
                    Text(
                        text = "0x"
                    )
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters
                )
            )
        }
    }
}

@Composable
fun ColorPickerDialog(
    defaultColor: Color? = null,
    onColorSelected: (Color) -> Unit,
    onDismissRequest: () -> Unit
) {
    var selectedColor: Color? by remember { mutableStateOf(defaultColor) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(stringResource(R.string.color_picker_title))
        },
        text = {
            ColorPickingLayout(defaultColor) { selectedColor = it }
        },
        confirmButton = {
            TextButton(
                enabled = selectedColor != null,
                onClick = { onColorSelected(selectedColor!!) }
            ) {
                Text(stringResource(R.string.color_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(stringResource(R.string.color_picker_dismiss))
            }
        }
    )
}

@Composable
fun ColorPicker(
    color: Color?,
    size: Dp,
    modifier: Modifier = Modifier,
    onColorSelected: (Color) -> Unit
) {
    var showingPickerDialog by remember { mutableStateOf(false) }
    if (showingPickerDialog)
        ColorPickerDialog(
            defaultColor = color,
            onColorSelected = onColorSelected,
            onDismissRequest = { showingPickerDialog = false }
        )

    ColorCircle(
        color ?: Color.Unspecified,
        size,
        false,
        Modifier
            .clickable { showingPickerDialog = true }
            .then(modifier)
    )
}

@Preview(showBackground = true)
@Composable
fun ColorPickingLayout_Preview() {
    ColorPickingLayout(
        selectedColor = legacyColorPalette[3],
        onColorSelected = {}
    )
}
