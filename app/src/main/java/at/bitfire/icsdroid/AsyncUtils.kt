package at.bitfire.icsdroid

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okio.IOException
import kotlin.coroutines.resumeWithException

/**
 * Executes the call suspendfully.
 *
 * FIXME - Should be removed and replaced with the official function when 5.0.0 stable is released.
 *
 * @see <a href="https://github.com/square/okhttp/blob/parent-5.0.0-alpha.12/okhttp-coroutines/src/jvmMain/kotlin/okhttp3/JvmCallExtensions.kt">Source</a>
 */
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun Call.executeAsync(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation {
        this.cancel()
    }
    this.enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            continuation.resume(value = response, onCancellation = { call.cancel() })
        }
    })
}
