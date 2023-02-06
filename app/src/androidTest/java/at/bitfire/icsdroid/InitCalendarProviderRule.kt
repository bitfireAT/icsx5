package at.bitfire.icsdroid

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.test.rule.GrantPermissionRule
import at.bitfire.icsdroid.Constants.TAG
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit ClassRule which initializes the AOSP CalendarProvider.
 * Needed for some "flaky" tests which would otherwise only succeed on second run.
 *
 * Currently tested on development machine (Ryzen) with Android 12 images (with/without Google Play).
 * Calendar provider behaves quite randomly, so it may or may not work. If you (the reader
 * if this comment) can find out on how to initialize the calendar provider so that the
 * tests are reliably run after `adb shell pm clear com.android.providers.calendar`,
 * please let us know!
 *
 * If you run tests manually, just make sure to ignore the first run after the calendar
 * provider has been accessed the first time.
 */
class InitCalendarProviderRule private constructor(): TestRule {

    companion object {
        fun getInstance(): RuleChain = RuleChain
            .outerRule(GrantPermissionRule.grant(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
            .around(InitCalendarProviderRule())
    }

    override fun apply(base: Statement, description: Description): Statement {
        Log.i(TAG, "Initializing calendar provider before running ${description.displayName}")
        return InitCalendarProviderStatement(base)
    }


    class InitCalendarProviderStatement(val base: Statement): Statement() {

        override fun evaluate() {
            if (Build.VERSION.SDK_INT < 31)
                Log.w(TAG, "Calendar provider initialization may or may not work. See InitCalendarProviderRule")

            base.evaluate()
        }
    }
}
