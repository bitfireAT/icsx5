package at.bitfire.icsdroid.ui.logic

import android.os.Build
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner

@Composable
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun BackInvokeHandler(
    enabled: Boolean = true,
    priority: Int = OnBackInvokedDispatcher.PRIORITY_DEFAULT,
    onBack: () -> Unit
) {
    val callback = remember {
        OnBackInvokedCallback {
            onBack()
        }
    }

    val activity = when (LocalLifecycleOwner.current) {
        is AppCompatActivity -> LocalLifecycleOwner.current as AppCompatActivity
        else -> {
            val context = LocalContext.current
            if (context is AppCompatActivity) {
                context
            } else {
                throw IllegalStateException("LocalLifecycleOwner is not AppCompatActivity")
            }
        }
    }

    if (enabled) {
        activity.onBackInvokedDispatcher.registerOnBackInvokedCallback(priority, callback)
    }

    LaunchedEffect(enabled) {
        if (!enabled) {
            activity.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(callback)
        }
    }

    DisposableEffect(activity.lifecycle, activity.onBackInvokedDispatcher) {
        onDispose {
            activity.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(callback)
        }
    }
}
