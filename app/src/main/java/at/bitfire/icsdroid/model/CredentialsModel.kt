package at.bitfire.icsdroid.model

import androidx.lifecycle.ViewModel
import at.bitfire.icsdroid.db.entity.Credential
import kotlinx.coroutines.flow.MutableStateFlow

class CredentialsModel : ViewModel() {
    val requiresAuth = MutableStateFlow(false)
    val username = MutableStateFlow<String?>(null)
    val password = MutableStateFlow<String?>(null)

    val isInsecure = MutableStateFlow(false)

    fun equalsCredential(credential: Credential) =
        username.value == credential.username
        && password.value == credential.password
}