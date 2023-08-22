package at.bitfire.icsdroid.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.tooling.preview.Preview
import at.bitfire.icsdroid.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@Composable
fun SyncIntervalDialog(
    currentInterval: Long,
    onSetSyncInterval: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val syncIntervalValues = stringArrayResource(R.array.set_sync_interval_seconds).map { it.toLong() }
    val currentIntervalIdx = syncIntervalValues.indexOf(currentInterval)

    MaterialAlertDialogBuilder(LocalContext.current)
        .setTitle(R.string.set_sync_interval_title)
        .setSingleChoiceItems(R.array.set_sync_interval_names, currentIntervalIdx) { dialog, newIdx ->
            onSetSyncInterval(syncIntervalValues[newIdx])
            dialog.dismiss()
        }
        .setOnDismissListener {
            onDismiss()
        }
        .show()
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