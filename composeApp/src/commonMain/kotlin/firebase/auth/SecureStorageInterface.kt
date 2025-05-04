package firebase.auth

/**
 * Interface for securely storing sensitive information like tokens
 * Each platform will implement this differently using their native secure storage mechanisms
 */
interface SecureStorage {
    /**
     * Save a string value securely
     * @param key Identifier for the value
     * @param value The string to store securely
     */
    fun saveString(key: String, value: String)

    /**
     * Retrieve a securely stored string value
     * @param key Identifier for the value
     * @return The stored string or null if not found
     */
    fun getString(key: String): String?

    /**
     * Remove a specific item from secure storage
     * @param key Identifier for the value to remove
     */
    fun remove(key: String)

    /**
     * Clear all values stored by this instance
     */
    fun clear()
}

/**
 * Platform-specific implementation factory
 * Each platform will provide its own implementation
 */
expect fun createSecureStorage(): SecureStorage