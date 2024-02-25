package at.bitfire.icsdroid.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import at.bitfire.icsdroid.db.entity.Credential

class CredentialsModel : ViewModel() {
    val requiresAuth = MutableLiveData(false)
    val username = MutableLiveData<String?>(null)
    val password = MutableLiveData<String?>(null)

    val isInsecure = MutableLiveData(false)

    fun equalsCredential(credential: Credential) =
        username.value == credential.username
        && password.value == credential.password
}