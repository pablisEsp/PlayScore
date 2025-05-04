package firebase.auth

import java.io.File
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * JVM implementation of SecureStorage using AES encryption
 * Uses a master key stored in the user's home directory
 */
class SecureStorageJvm : SecureStorage {
    private val storageDir = File(System.getProperty("user.home"), ".playscore/secure")
    private val keyFile = File(storageDir, "master.key")
    private val dataFile = File(storageDir, "secure.properties")
    private val properties = Properties()
    private val masterKey: SecretKey

    init {
        // Ensure storage directory exists
        storageDir.mkdirs()

        // Initialize or load the master encryption key
        masterKey = if (keyFile.exists()) {
            loadMasterKey()
        } else {
            generateMasterKey()
        }

        // Load existing data if available
        if (dataFile.exists()) {
            dataFile.inputStream().use { properties.load(it) }
        }
    }

    override fun saveString(key: String, value: String) {
        val encryptedValue = encrypt(value)
        properties.setProperty(key, encryptedValue)
        saveProperties()
    }

    override fun getString(key: String): String? {
        val encryptedValue = properties.getProperty(key) ?: return null
        return try {
            decrypt(encryptedValue)
        } catch (e: Exception) {
            println("Error decrypting value for key '$key': ${e.message}")
            null
        }
    }

    override fun remove(key: String) {
        properties.remove(key)
        saveProperties()
    }

    override fun clear() {
        properties.clear()
        saveProperties()
    }

    private fun saveProperties() {
        dataFile.outputStream().use { properties.store(it, "Secure Storage") }
    }

    private fun generateMasterKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256, SecureRandom())
        val key = keyGen.generateKey()

        // Save the key for future use
        val encodedKey = key.encoded
        keyFile.writeBytes(encodedKey)

        return key
    }

    private fun loadMasterKey(): SecretKey {
        val encodedKey = keyFile.readBytes()
        return SecretKeySpec(encodedKey, "AES")
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

        // Generate random IV
        val ivBytes = ByteArray(16)
        SecureRandom().nextBytes(ivBytes)
        val ivSpec = IvParameterSpec(ivBytes)

        // Encrypt
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, ivSpec)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray())

        // Combine IV and encrypted data
        val combined = ByteArray(ivBytes.size + encryptedBytes.size)
        System.arraycopy(ivBytes, 0, combined, 0, ivBytes.size)
        System.arraycopy(encryptedBytes, 0, combined, ivBytes.size, encryptedBytes.size)

        return Base64.getEncoder().encodeToString(combined)
    }

    private fun decrypt(encryptedValue: String): String {
        // Decode from Base64
        val combined = Base64.getDecoder().decode(encryptedValue)

        // Extract IV
        val ivBytes = ByteArray(16)
        System.arraycopy(combined, 0, ivBytes, 0, ivBytes.size)
        val ivSpec = IvParameterSpec(ivBytes)

        // Extract encrypted data
        val encryptedBytes = ByteArray(combined.size - ivBytes.size)
        System.arraycopy(combined, ivBytes.size, encryptedBytes, 0, encryptedBytes.size)

        // Decrypt
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, masterKey, ivSpec)
        val decryptedBytes = cipher.doFinal(encryptedBytes)

        return String(decryptedBytes)
    }
}

// Platform-specific implementation factory
actual fun createSecureStorage(): SecureStorage = SecureStorageJvm()