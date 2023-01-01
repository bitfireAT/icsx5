package at.bitfire.icsdroid.ui.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.doAsync
import kotlinx.coroutines.Job

class EditSubscriptionModel(application: Application): AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)

    private val dao = database.subscriptionsDao()

    val subscription = MutableLiveData<Subscription>()

    /**
     * Loads a subscription from the database into [subscription] from its [Subscription.id].
     * @author Arnau Mora
     * @since 20230101
     * @param id The id of the subscription.
     * @return A [Job] for supervising the status of the request.
     */
    fun load(id: Long) = doAsync {
        dao.getById(id)?.let { subscription.postValue(it) }
    }
}