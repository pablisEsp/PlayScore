package viewmodel

import androidx.lifecycle.ViewModel
import firebase.auth.FirebaseAuthInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class ForgotPasswordViewModel(
    private val coroutineContext: CoroutineContext,
    private val auth: FirebaseAuthInterface
) : ViewModel() {
    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _isResetEmailSent = MutableStateFlow(false)
    val isResetEmailSent = _isResetEmailSent.asStateFlow()

    fun onEmailChanged(value: String) {
        _email.value = value
    }

    fun resetPassword() {
        if (_email.value.isBlank()) {
            _message.value = "Email cannot be empty"
            return
        }

        _isLoading.value = true
        _message.value = null

        CoroutineScope(coroutineContext).launch {
            try {
                if (auth.sendPasswordResetEmail(_email.value)) {
                    _message.value = "If an account with that email exists, a password reset link has been sent"
                    _isResetEmailSent.value = true
                } else {
                    _message.value = "Failed to send password reset email"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearState() {
        _isResetEmailSent.value = false
        _message.value = null
    }
}