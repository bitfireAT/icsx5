package at.bitfire.icsdroid.db.interfaces

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.WorkerThread
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.entity.Subscription

/**
 * Adds the subscription to the database, and creates the corresponding Android Calendar.
 * @param context The context that is making the request.
 * @return The uri of the newly created Android Calendar.
 * @throws NullPointerException If the context given doesn't have a valid provider.
 * @throws Exception If the calendar could not be created.
 */
@WorkerThread
suspend fun Subscription.create(context: Context): Uri {
    Log.v(Constants.TAG, "Adding subscription to database...")
    AppDatabase.getInstance(context)
        .subscriptionsDao()
        .add(this)

    Log.v(Constants.TAG, "Adding subscription to system...")
    return createAndroidCalendar(context)
}
