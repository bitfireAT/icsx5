package at.bitfire.icsdroid.ui.reusable

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun TabWithTextAndIcon(
    selected: Boolean,
    icon: ImageVector,
    @StringRes text: Int,
    onClick: suspend CoroutineScope.() -> Unit,
) {
    Tab(
        selected = selected,
        onClick = { CoroutineScope(Dispatchers.Main).launch(block = onClick) },
        modifier = Modifier.padding(bottom = 4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(text),
        )
        Text(text = stringResource(text))
    }
}
