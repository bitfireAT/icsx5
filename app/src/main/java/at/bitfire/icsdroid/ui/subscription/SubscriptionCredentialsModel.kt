package at.bitfire.icsdroid.ui.subscription

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SubscriptionCredentialsModel : ViewModel() {
    var originalRequiresAuth: Boolean? = null
    var originalUsername: String? = null
    var originalPassword: String? = null

    val requiresAuth = MutableLiveData<Boolean>()
    val username = MutableLiveData<String>()
    val password = MutableLiveData<String>()

    init {
        requiresAuth.value = false
    }

    val isDirty = MediatorLiveData<Boolean>().apply {
        addSource(requiresAuth) { value = dirty() }
        addSource(username) { value = dirty() }
        addSource(password) { value = dirty() }
    }

    private fun dirty() = requiresAuth.value != originalRequiresAuth ||
        username.value != originalUsername ||
        password.value != originalPassword
}
