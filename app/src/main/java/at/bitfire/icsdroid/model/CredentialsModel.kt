package at.bitfire.icsdroid.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CredentialsModel : ViewModel() {
    val requiresAuth = MutableLiveData(false)
    val username = MutableLiveData("")
    val password = MutableLiveData("")

    val isInsecure = MutableLiveData(false)
}