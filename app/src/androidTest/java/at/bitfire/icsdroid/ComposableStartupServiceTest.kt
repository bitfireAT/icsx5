package at.bitfire.icsdroid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import at.bitfire.icsdroid.service.ComposableStartupService
import at.bitfire.icsdroid.service.ComposableStartupService.Companion.FLAG_DONATION_DIALOG
import org.junit.Test

class ComposableStartupServiceTest {
    private fun createService(vararg flags: Int): ComposableStartupService {
        return object : ComposableStartupService {
            override val flags: Int = flags.sum()

            @Composable
            override fun shouldShow(): State<Boolean> {
                TODO("Not yet implemented")
            }

            @Composable
            override fun Content() {
                TODO("Not yet implemented")
            }
        }
    }

    @Test
    fun testHasFlagNoFlags() {
        val service = createService()
        assert(!service.hasFlag(FLAG_DONATION_DIALOG))
    }

    @Test
    fun testHasFlagSingleFlag() {
        val service = createService(FLAG_DONATION_DIALOG)
        assert(service.hasFlag(FLAG_DONATION_DIALOG))
    }

    @Test
    fun testHasFlagMultipleFlag() {
        val service = createService(FLAG_DONATION_DIALOG, 0b10)
        assert(service.hasFlag(FLAG_DONATION_DIALOG))
        assert(service.hasFlag(0b10))
    }
}
