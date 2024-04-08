package at.bitfire.icsdroid

import android.content.Context
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.entity.Subscription
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BackupTest {
    companion object {
        val appContext: Context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }

        private val subscription1 =
            Subscription(1, null, Uri.parse("https://example.com/calendar1.ics"), null, "Example 1")
        private val subscription2 =
            Subscription(2, null, Uri.parse("https://example.com/calendar2.ics"), null, "Example 2")
    }

    @Before
    fun prepare_database() {
        // Change the database name
        AppDatabase.databaseName = "icsx5-test"

        // Create the database, and add some sample subscriptions
        val database = AppDatabase.getInstance(appContext)

        val subscriptionsDao = database.subscriptionsDao()
        subscriptionsDao.add(subscription1)
        subscriptionsDao.add(subscription2)
    }

    @Test
    fun test_import_export() {
        // Get the data stored in the db
        // Note: This closes the database
        val data = runBlocking { AppDatabase.readAllData(appContext) }

        // Clear the data to make sure the test is not confused
        AppDatabase.getInstance(appContext).clearAllTables()

        // Import the data
        runBlocking { AppDatabase.recreateFromFile(appContext) { data.inputStream() } }

        // Assert
        val subscriptionsDao = AppDatabase.getInstance(appContext).subscriptionsDao()
        val subscriptions = subscriptionsDao.getAll()
        assertEquals(2, subscriptions.size)
        assertEquals(subscription1, subscriptions[0])
        assertEquals(subscription2, subscriptions[1])
    }

    @After
    fun dispose_database() {
        AppDatabase.getInstance(appContext).clearAllTables()
    }
}
