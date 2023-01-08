package at.bitfire.icsdroid.ui.pages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.ui.model.CreateSubscriptionModel
import at.bitfire.icsdroid.ui.reusable.ColorPicker

@Preview
@Composable
@ExperimentalMaterial3Api
fun CreateSubscriptionDetailsPage(model: CreateSubscriptionModel = viewModel()) {
    var displayName by model.displayName
    var color by model.color

    OutlinedTextField(
        value = displayName,
        onValueChange = { displayName = it },
        modifier = Modifier
            .fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            autoCorrect = true,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions {  },
        singleLine = true,
        maxLines = 1,
        label = { Text(stringResource(R.string.add_calendar_title_hint)) },
        leadingIcon = {
            ColorPicker(
                color = color,
                modifier = Modifier
                    .padding(8.dp),
            ) { color = it }
        },
    )
}
