package viewmodel

import androidx.lifecycle.ViewModel
import data.model.User
import firebase.auth.FirebaseAuthInterface
import firebase.database.FirebaseDatabaseInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class AccountSettingsViewModel(
    private val coroutineContext: CoroutineContext,
    private val database: FirebaseDatabaseInterface,
    private val auth: FirebaseAuthInterface,
    private val userViewModel: UserViewModel
) : ViewModel() {
    private val _userInfo = MutableStateFlow<User?>(null)
    val userInfo = _userInfo.asStateFlow()

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess = _isSuccess.asStateFlow()

    fun loadUserInfo() {
        _isLoading.value = true
        _message.value = null

        CoroutineScope(coroutineContext).launch {
            try {
                val currentUser = userViewModel.currentUser.value
                currentUser?.let { user ->
                    _userInfo.value = user
                    _username.value = user.username
                    _name.value = user.name
                }
            } catch (e: Exception) {
                _message.value = "Error loading user information: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onUsernameChanged(value: String) {
        _username.value = value
    }

    fun onNameChanged(value: String) {
        _name.value = value
    }

    fun saveChanges() {
        // Validate username
        if (_username.value.length < 3) {
            _message.value = "Username must be at least 3 characters"
            return
        }

        _isLoading.value = true
        _message.value = null

        CoroutineScope(coroutineContext).launch {
            try {
                val currentUser = _userInfo.value
                if (currentUser != null) {
                    // Check if username is being changed and if it's available
                    if (_username.value != currentUser.username) {
                        val isAvailable = database.checkUsernameAvailable(_username.value)
                        if (!isAvailable) {
                            _message.value = "Username is already taken"
                            _isLoading.value = false
                            return@launch
                        }
                    }

                    // Prepare update map
                    val updates = mutableMapOf<String, Any?>()

                    if (_username.value != currentUser.username) {
                        updates["username"] = _username.value
                    }

                    if (_name.value != currentUser.name) {
                        updates["name"] = _name.value
                    }

                    // Only update if there are changes
                    if (updates.isNotEmpty()) {
                        val success = database.updateUserData(currentUser.id, updates)
                        if (success) {
                            _message.value = "Profile updated successfully"
                            _isSuccess.value = true

                            // Update local user data in UserViewModel
                            userViewModel.loadCurrentUser()
                        } else {
                            _message.value = "Failed to update profile"
                        }
                    } else {
                        _message.value = "No changes to save"
                    }
                }
            } catch (e: Exception) {
                _message.value = "Error updating profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}