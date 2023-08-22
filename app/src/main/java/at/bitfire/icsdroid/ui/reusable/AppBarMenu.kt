package at.bitfire.icsdroid.ui.reusable

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import at.bitfire.icsdroid.R

/**
 * Defines the looks and functionality of an icon from an AppBar.
 *
 * @param icon The icon to display when the icon is visible in the action bar.
 * @param title The content description of the icon when visible as image, and the text that
 * identifies the item when in an overflow menu.
 * @param isVisible Using the value given, should return whether to display the icon or not. If null
 * the icon is always displayed.
 *
 * @see AppBarMenu
 */
data class AppBarIcon<T> (
    val icon: ImageVector,
    val title: String,
    val onClick: () -> Unit,
    val isVisible: @Composable ((T) -> Boolean)? = null
)

/**
 * Provides a way to dynamically update the action buttons of an AppBar. If there are more than
 * [maxIcons] visible icons, they are hidden behind an overflow menu.
 *
 * @param icons The list of icons to display. The order of this list will be the one of the icons
 * shown, from left to right.
 * @param maxIcons The maximum amount of icons to display, if more than this amount of icons is
 * visible, they will be hidden behind an overflow menu.
 */
@Composable
@Suppress("UnusedReceiverParameter")
fun <T> RowScope.AppBarMenu(
    icons: List<AppBarIcon<T>>,
    value: T,
    maxIcons: Int = 3
) {
    val visibleIcons = icons.filter { it.isVisible?.invoke(value) ?: true }

    /** Icons that should be displayed in the action bar */
    val barIcons = if (visibleIcons.size <= maxIcons)
        // If icon count is less or equal than maxIcons, display all of them
        visibleIcons
    else
        // Otherwise display the first maxIcons icons
        visibleIcons.subList(0, maxIcons - 1)
    /** Icons that should be displayed in the overflow menu */
    val overflowIcons = if (visibleIcons.size <= maxIcons)
        // If there are less than maxIcons, don't overflow
        emptyList()
    else
        visibleIcons.subList(maxIcons - 1, visibleIcons.size)

    // Iterate as much items as maxIcons. If there are less than maxIcons, iterate that amount
    for (icon in barIcons) {
        IconButton(
            onClick = icon.onClick
        ) {
            Icon(icon.icon, icon.title)
        }
    }

    // If there are more than maxIcons, create the overflow menu
    if (overflowIcons.isNotEmpty()) {
        var expanded by remember { mutableStateOf(false) }

        IconButton(onClick = { expanded = !expanded }) {
            Icon(Icons.Rounded.MoreVert, stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (icon in overflowIcons) {
                DropdownMenuItem(onClick = icon.onClick) {
                    Text(icon.title)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppBarMenu_Preview(
    @PreviewParameter(AppBarMenuPreviewProvider::class) value: Int
) {
    Row {
        AppBarMenu(
            icons = listOf(
                // Always visible
                AppBarIcon(
                    Icons.Rounded.Add,
                    "Always visible",
                    {  }
                ),
                // Always visible
                AppBarIcon(
                    Icons.Rounded.AddCircle,
                    "Always visible",
                    {  },
                    { true }
                ),
                // Always hidden
                AppBarIcon(
                    Icons.Rounded.Close,
                    "Always hidden",
                    {  },
                    { false }
                ),
                // Depends on value
                AppBarIcon(
                    Icons.Rounded.Warning,
                    "Depends on value 1",
                    {  },
                    { it == 2 }
                ),
                // Shown in overflow if previous item displayed
                AppBarIcon(
                    Icons.Rounded.Info,
                    "Overflowing",
                    {  }
                )
            ),
            value
        )
    }
}

class AppBarMenuPreviewProvider: PreviewParameterProvider<Int> {
    override val values: Sequence<Int> = sequenceOf(1, 2)
}
