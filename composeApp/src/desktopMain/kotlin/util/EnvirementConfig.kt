package util

object EnvironmentConfig {
    fun getEnv(key: String, defaultValue: String? = null): String? {
        return System.getenv(key) ?: defaultValue
    }

    fun getRequiredEnv(key: String): String {
        return System.getenv(key) ?: throw IllegalStateException("Required environment variable $key not set")
    }

    val AUTH_API_URL: String
        get() = getEnv("AUTH_API_URL") ?: "http://localhost:3000/api/auth"
}