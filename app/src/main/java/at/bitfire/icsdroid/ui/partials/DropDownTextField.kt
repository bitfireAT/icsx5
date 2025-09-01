package at.bitfire.icsdroid.ui.partials

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.toSize


@Composable
fun DropDownTextField(
    value: String?,
    onValueChange: (String) -> Unit,
    options: Map<String, String>,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(Size.Zero) }

    Column {
        OutlinedTextField(
            value = value ?: "",
            onValueChange = onValueChange,
            keyboardActions = keyboardActions,
            modifier = modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    // Remember text field size
                    textFieldSize = coordinates.size.toSize()
                },
            label = label,
            trailingIcon = {
                // Open/Close dropdown icon
                Icon(
                    imageVector = if (expanded)
                        Icons.Filled.KeyboardArrowUp
                    else
                        Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = modifier.clickable { expanded = !expanded }
                )
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = modifier.width(with(LocalDensity.current) {
                // Make drop down menu same length as text field
                textFieldSize.width.toDp()
            })
        ) {
            options.forEach { (label, option) ->
                DropdownMenuItem(
                    text = { Text(text = label) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}