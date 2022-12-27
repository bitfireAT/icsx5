package at.bitfire.icsdroid

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

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

/**
 * Runs the given block of code in the IO thread using the view model scope ([ViewModel.viewModelScope]).
 * @author Arnau Mora
 * @since 20221227
 * @param block The block of code to be run.
 * @return A job that notifies about the result of the work.
 */
fun ViewModel.doAsync(block: suspend CoroutineScope.() -> Unit) = viewModelScope.launch(context = Dispatchers.IO, block = block)
