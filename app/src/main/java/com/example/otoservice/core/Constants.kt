package com.example.otoservice.core

/**
 * Application-wide constants
 * Security-sensitive values are obfuscated
 */
object Constants {
    
    // SharedPreferences file names
    const val PREFS_LICENSE = "license_prefs"
    const val PREFS_APP_SELECTION = "app_selection_prefs"
    const val PREFS_LOCATION = "location_prefs"
    const val PREFS_DEVICE_INFO = "device_info_prefs"
    
    // License SharedPreferences keys
    const val KEY_LICENSE_CODE = "license_code"
    const val KEY_LICENSE_ACTIVE = "license_active"
    const val KEY_LICENSE_EXPIRY_TIME = "license_expiry_time_millis"
    const val KEY_LICENSE_EXPIRY = KEY_LICENSE_EXPIRY_TIME
    const val KEY_LICENSE_ACTIVATION_TIME = "license_activation_time_millis"
    const val KEY_LAST_KNOWN_WALL_TIME = "last_known_wall_time_millis"
    const val KEY_LAST_KNOWN_ELAPSED_TIME = "last_known_elapsed_realtime_millis"
    const val KEY_TIME_TAMPER_LOCK = "time_tamper_permanent_lock"
    const val KEY_CLONE_TAMPER_LOCK = "clone_tamper_permanent_lock"
    const val KEY_CODE_TAMPER_LOCK = "code_tamper_permanent_lock"
    const val KEY_DEVICE_INFO_SENT = "device_info_sent"
    
    // Time tamper thresholds
    const val BACKWARD_THRESHOLD_MS = 60_000L // 1 minute
    const val MAX_FORWARD_JUMP_MS = 7L * 24L * 60L * 60L * 1000L // 7 days
    
    // License code format
    const val LICENSE_CODE_LENGTH_MIN = 20
    const val LICENSE_CODE_LENGTH_MAX = 40
    
    // Auto-reply settings
    const val MAX_SELECTED_APPS = 6
    const val MIN_REPLY_DELAY_SECONDS = 5
    const val MAX_REPLY_DELAY_SECONDS = 300
    const val DEFAULT_REPLY_DELAY_SECONDS = 10
    const val DEFAULT_REPLY_MESSAGE = "Şu anda meşgulüm, daha sonra yazacağım."
    
    // Location settings
    const val MIN_LOCATION_UPDATE_INTERVAL_SECONDS = 15
    const val MAX_LOCATION_UPDATE_INTERVAL_SECONDS = 60
    const val DEFAULT_LOCATION_UPDATE_INTERVAL_SECONDS = 25
    const val MIN_DISTRICT_DURATION_MINUTES = 5
    const val MAX_DISTRICT_DURATION_MINUTES = 180
    const val DEFAULT_DISTRICT_DURATION_MINUTES = 20
    
    // Telegram Bot Configuration (obfuscated)
    // Real values: Bot Token and Chat ID are split and encoded
    private val BOT_TOKEN_PARTS = arrayOf(
        "7996610464",
        "AAHMIs2CwF0--eB4_8S4X1",
        "C5b5kZRYNQMs"
    )
    
    private const val CHAT_ID_ENCODED = "6466581970"
    
    /**
     * Retrieves the Telegram bot token (obfuscated assembly)
     */
    fun getTelegramBotToken(): String {
        return BOT_TOKEN_PARTS.joinToString("-")
    }
    
    /**
     * Retrieves the Telegram admin chat ID (obfuscated)
     */
    fun getTelegramChatId(): String {
        return CHAT_ID_ENCODED
    }
    
    // Telegram API endpoint
    const val TELEGRAM_API_BASE_URL = "https://api.telegram.org"
    
    // Secret seed for license algorithm (placeholder - will be used in obfuscated form)
    // This is intentionally not a simple string to make reverse engineering harder
    private val SEED_COMPONENTS = byteArrayOf(
        0x53, 0x45, 0x43, 0x52, 0x45, 0x54, // "SECRET"
        0x5F, 0x53, 0x45, 0x45, 0x44, 0x5F, // "_SEED_"
        0x50, 0x4C, 0x41, 0x43, 0x45, 0x48, // "PLACEH"
        0x4F, 0x4C, 0x44, 0x45, 0x52 // "OLDER"
    )
    
    /**
     * Get the secret seed for license validation
     * In production, this should be further obfuscated
     */
    fun getSecretSeed(): String {
        return String(SEED_COMPONENTS)
    }
    
    // Anti-tamper signature (checksum of critical components)
    // This should match a computed runtime fingerprint
    const val EXPECTED_CODE_SIGNATURE = "OTO_SERVICE_V1_SIGNATURE_2024"
}
