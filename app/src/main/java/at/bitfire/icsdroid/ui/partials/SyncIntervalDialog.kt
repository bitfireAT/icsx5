/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.ui.partials

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import at.bitfire.icsdroid.R

@Composable
fun SyncIntervalDialog(
    currentInterval: Long,
    onSetSyncInterval: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val syncIntervalNames = stringArrayResource(R.array.set_sync_interval_names)
    val syncIntervalValues = stringArrayResource(R.array.set_sync_interval_seconds).map { it.toLong() }
    val currentIntervalIdx = syncIntervalValues.indexOf(currentInterval)

    GenericAlertDialog(
        title = stringResource(R.string.set_sync_interval_title),
        confirmButton = stringResource(android.R.string.ok) to onDismiss,
        dismissButton = stringResource(android.R.string.cancel) to onDismiss,
        onDismissRequest = onDismiss,
        content = {
            LazyColumn {
                itemsIndexed(syncIntervalNames) { index, name ->
                    ListItem(
                        modifier = Modifier.clickable {
                            onSetSyncInterval(syncIntervalValues[index])
                        },
                        headlineContent = { Text(name) },
                        trailingContent = {
                            Icon(
                                imageVector = if (currentIntervalIdx == index)
                                    Icons.Filled.RadioButtonChecked
                                else
                                    Icons.Filled.RadioButtonUnchecked,
                                contentDescription = null
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        }
    )
}

@Preview
@Composable
fun SyncIntervalDialog_Preview() {
    SyncIntervalDialog(
        -1,     // only manually
        onSetSyncInterval = {},
        onDismiss = {}
    )
}