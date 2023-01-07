package at.bitfire.icsdroid.ui.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.ui.model.CreateSubscriptionModel

@Composable
@ExperimentalMaterial3Api
fun CreateSubscriptionFilePage(model: CreateSubscriptionModel) {
    val fileName by model.fileName

    OutlinedTextField(
        value = fileName ?: stringResource(R.string.add_calendar_pick_file),
        onValueChange = {},
        enabled = false,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            disabledBorderColor = MaterialTheme.colorScheme.primary,
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledSupportingTextColor = MaterialTheme.colorScheme.onSurface,
            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurface,
            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface,
        ),
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.FileOpen,
                contentDescription = stringResource(R.string.add_calendar_pick_file),
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clickable { model.pickFile() },
    )
}
