package at.bitfire.icsdroid

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Runs the code in [block] asynchronously, in the IO thread.
 * @author Arnau Mora
 * @since 20221227
 * @param block The block of code to be run.
 * @return A job that notifies about the result of the work.
 */
fun doAsync(@WorkerThread block: suspend CoroutineScope.() -> Unit) = CoroutineScope(Dispatchers.IO).launch(block = block)

/**
 * Runs the code in [block] in the UI thread.
 * @author Arnau Mora
 * @since 20221227
 * @param block The block of code to be run.
 */
suspend fun <T> ui(@UiThread block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.Main, block)
