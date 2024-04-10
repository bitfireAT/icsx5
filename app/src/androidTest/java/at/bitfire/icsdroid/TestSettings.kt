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
        val settings = Settings(appContext)
        var forceDarkMode = false
        CoroutineScope(Dispatchers.IO).launch {
            settings.forceDarkModeFlow().collect { forceDarkMode = it }
        }
        settings.forceDarkMode(true)
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
