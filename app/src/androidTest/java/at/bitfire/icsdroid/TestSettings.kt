package at.bitfire.icsdroid

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test

class TestSettings {
    companion object {
        val appContext: Context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }
    }

    @Test
    fun testForceDarkModeFlow() {
        // Initialize settings
        val settings = Settings(appContext)
        // Create a variable for storing the value of the preference when updated
        var forceDarkMode = false
        // Launch a coroutine to collect the flow
        CoroutineScope(Dispatchers.IO).launch {
            settings.forceDarkModeFlow().collect { forceDarkMode = it }
        }
        // Set the preference to true
        settings.forceDarkMode(true)
        // Wait for the flow to update, or throw timeout
        runBlocking {
            withTimeout(1000) {
                while (!forceDarkMode) {
                    delay(10)
                }
            }
        }
        assert(forceDarkMode)
    }
}
