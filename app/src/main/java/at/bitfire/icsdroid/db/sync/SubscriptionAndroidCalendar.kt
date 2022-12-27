package at.bitfire.icsdroid.db.sync

import android.accounts.Account
import android.content.ContentProviderClient
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.AndroidCalendarFactory

class SubscriptionAndroidCalendar private constructor(
    account: Account,
    provider: ContentProviderClient,
    id: Long,
): AndroidCalendar<SubscriptionAndroidEvent>(account, provider, SubscriptionAndroidEvent.Factory(), id) {
    class Factory: AndroidCalendarFactory<SubscriptionAndroidCalendar> {
        override fun newInstance(account: Account, provider: ContentProviderClient, id: Long): SubscriptionAndroidCalendar =
            SubscriptionAndroidCalendar(account, provider, id)
    }
}
