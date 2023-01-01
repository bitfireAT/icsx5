package at.bitfire.icsdroid.ui.model

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.work.WorkInfo
import at.bitfire.icsdroid.SyncWorker
import at.bitfire.icsdroid.db.AppDatabase

class CalendarModel(application: Application): AndroidViewModel(application) {
    /**
     * A reference to the instance of the Room database.
     * @since 20221227
     */
    private val database = AppDatabase.getInstance(application)

    /**
     * Gets updated with the results of the permissions check. If the array is empty, it means that no permissions shall be asked for. Otherwise, the user
     * must be requested all the permissions given.
     * @since 20221225
     * @see checkPermissions
     */
    val requiredPermissions: MutableLiveData<Array<String>> = MutableLiveData(emptyArray())

    /** whether there are running sync workers */
    val isRefreshing = Transformations.map(SyncWorker.liveStatus(application)) { workInfos ->
        workInfos.any { it.state == WorkInfo.State.RUNNING }
    }

    /**
     * Provides a LiveData that gets updated with all the subscriptions made in the database.
     * @since 20221225
     */
    val subscriptions = database.subscriptionsDao().getAllLive()

    /**
     * Checks if all the required permissions are granted. This includes:
     * - Notification permission (API 33+)
     * Updates [requiredPermissions] with the result of the check.
     * @since 20221225
     * @see requiredPermissions
     */
    fun checkPermissions() {
        val permissions = arrayListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)

        requiredPermissions.postValue(permissions.toTypedArray())
    }
}