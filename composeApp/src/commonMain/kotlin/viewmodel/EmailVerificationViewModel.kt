package viewmodel

import androidx.lifecycle.ViewModel
import firebase.auth.FirebaseAuthInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class EmailVerificationViewModel(
    private val coroutineContext: CoroutineContext,
    private val auth: FirebaseAuthInterface
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    fun setEmail(email: String) {
        _email.value = email
    }

    fun resendVerificationEmail() {
        _isLoading.value = true
        _message.value = null

        CoroutineScope(coroutineContext).launch {
            try {
                if (auth.sendEmailVerification()) {
                    _message.value = "Verification email sent successfully"
                } else {
                    _message.value = "Failed to send verification email"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}