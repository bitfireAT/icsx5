package at.bitfire.icsdroid.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import at.bitfire.icsdroid.db.entity.Credential
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CredentialsModel @Inject constructor(): ViewModel() {
    data class UiState(
        val requiresAuth: Boolean = false,
        val username: String? = null,
        val password: String? = null,
        val isInsecure: Boolean = false
    )

    var uiState by mutableStateOf(UiState())
        private set

    fun setRequiresAuth(value: Boolean) {
        uiState = uiState.copy(requiresAuth = value)
    }

    fun setUsername(value: String?) {
        uiState = uiState.copy(username = value)
    }

    fun setPassword(value: String?) {
        uiState = uiState.copy(password = value)
    }

    fun clearCredentials() {
        uiState = uiState.copy(username = null, password = null)
    }

    fun setIsInsecure(value: Boolean) {
        uiState = uiState.copy(isInsecure = value)
    }

    fun equalsCredential(credential: Credential) =
        uiState.username == credential.username
        && uiState.password == credential.password
}
