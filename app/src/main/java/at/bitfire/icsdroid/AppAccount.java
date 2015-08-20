package at.bitfire.icsdroid;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.CalendarContract;

public class AppAccount {

    public static final Account account = new Account("ICSdroid", "at.bitfire.icsdroid");

    public static boolean isAvailable(Context context) {
        AccountManager am = AccountManager.get(context);
        return am.getAccountsByType(account.type).length > 0;
    }

    public static boolean isSyncActive(Context context) {
        return ContentResolver.isSyncActive(AppAccount.account, CalendarContract.AUTHORITY);
    }

}
