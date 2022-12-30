package at.bitfire.icsdroid

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

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
 * @return The result of [block].
 */
suspend fun <T> ui(@UiThread block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.Main, block)

/**
 * Runs the code in [block] in the UI thread, blocking the current thread.
 * @author Arnau Mora
 * @since 20221230
 * @param block The block of code to be run.
 * @return The result of [block].
 */
fun <T> blockingUi(@UiThread block: suspend CoroutineScope.() -> T): T = runBlocking(Dispatchers.Main, block)

/**
 * Runs the given block of code in the IO thread using the view model scope ([ViewModel.viewModelScope]).
 * @author Arnau Mora
 * @since 20221227
 * @param block The block of code to be run.
 * @return A job that notifies about the result of the work.
 */
fun ViewModel.doAsync(block: suspend CoroutineScope.() -> Unit) = viewModelScope.launch(context = Dispatchers.IO, block = block)
