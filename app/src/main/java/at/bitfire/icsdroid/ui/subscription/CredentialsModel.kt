package at.bitfire.icsdroid.ui.subscription

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CredentialsModel : ViewModel() {
    var originalRequiresAuth: Boolean? = null
    var originalUsername: String? = null
    var originalPassword: String? = null

    val requiresAuth = MutableLiveData<Boolean>()
    val username = MutableLiveData<String>()
    val password = MutableLiveData<String>()

    init {
        requiresAuth.value = false
    }

    fun dirty() = requiresAuth.value != originalRequiresAuth ||
            username.value != originalUsername ||
            password.value != originalPassword
}
