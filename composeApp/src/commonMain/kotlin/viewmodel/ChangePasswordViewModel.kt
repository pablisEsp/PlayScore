package viewmodel

import androidx.lifecycle.ViewModel
import firebase.auth.FirebaseAuthInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class ChangePasswordViewModel(
    private val coroutineContext: CoroutineContext,
    private val auth: FirebaseAuthInterface
) : ViewModel() {
    private val _currentPassword = MutableStateFlow("")
    val currentPassword = _currentPassword.asStateFlow()

    private val _newPassword = MutableStateFlow("")
    val newPassword = _newPassword.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword = _confirmPassword.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess = _isSuccess.asStateFlow()

    fun onCurrentPasswordChanged(value: String) {
        _currentPassword.value = value
    }

    fun onNewPasswordChanged(value: String) {
        _newPassword.value = value
    }

    fun onConfirmPasswordChanged(value: String) {
        _confirmPassword.value = value
    }

    fun changePassword() {
        if (_currentPassword.value.isBlank() || _newPassword.value.isBlank() || _confirmPassword.value.isBlank()) {
            _message.value = "All fields are required"
            return
        }

        if (_newPassword.value != _confirmPassword.value) {
            _message.value = "New passwords don't match"
            return
        }

        if (_newPassword.value.length < 6) {
            _message.value = "Password must be at least 6 characters"
            return
        }

        _isLoading.value = true
        _message.value = null

        CoroutineScope(coroutineContext).launch {
            try {
                val result = auth.updatePassword(_currentPassword.value, _newPassword.value)
                if (result) {
                    _message.value = "Password changed successfully"
                    _isSuccess.value = true
                    clearPasswords()
                } else {
                    _message.value = "Failed to change password. Please verify your current password."
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun clearPasswords() {
        _currentPassword.value = ""
        _newPassword.value = ""
        _confirmPassword.value = ""
    }

    fun clearMessage() {
        _message.value = null
    }
}