package at.bitfire.icsdroid.ui.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.entity.Subscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
    fun load(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        dao.getById(id)?.let { subscription.postValue(it) }
    }
}