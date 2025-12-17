package com.example.otoservice.deviceinfo

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.otoservice.core.PreferencesManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * Collects device information for license request
 * This data is sent to Telegram bot on first launch
 */
class DeviceInfoCollector(private val context: Context) {
    
    private val prefsManager = PreferencesManager(context)
    
    /**
     * Generates a unique device fingerprint
     * Format: DEV-XXXX-YYYY-ZZZZ
     */
    fun generateDeviceFingerprint(): String {
        // IMPORTANT: This must return the SAME value every time for the same device
        // Otherwise license validation will fail!
        
        // Use existing device ID from preferences (generated once and stored)
        val storedFingerprint = prefsManager.deviceInfoPrefs.getString("device_fingerprint", null)
        if (storedFingerprint != null && storedFingerprint.startsWith("DEV-")) {
            return storedFingerprint
        }
        
        // Generate new fingerprint (first time only)
        val deviceId = prefsManager.getDeviceId()
        
        // Get Android ID for additional uniqueness
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
        
        // Combine device info to create fingerprint
        // Use MD5 for deterministic hashing
        val combined = "${Build.MANUFACTURER}-${Build.MODEL}-$androidId-$deviceId"
        val fingerprint = generateFingerprintFromString(combined)
        
        // Store it so it never changes
        prefsManager.deviceInfoPrefs.edit()
            .putString("device_fingerprint", fingerprint)
            .apply()
        
        return fingerprint
    }
    
    fun getUniqueDeviceId(): String {
        return generateDeviceFingerprint()
    }
    
    private fun generateFingerprintFromString(input: String): String {
        // Use MD5 for deterministic hashing
        val md = java.security.MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        val hash = digest.joinToString("") { "%02x".format(it) }
        
        // Take first 12 hex characters
        val hex12 = hash.take(12)
        
        // Format as DEV-XXXX-YYYY-ZZZZ
        return "DEV-${hex12.substring(0, 4)}-${hex12.substring(4, 8)}-${hex12.substring(8, 12)}".uppercase()
    }
    
    /**
     * Gets device brand and model
     */
    fun getDeviceBrandModel(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }
    
    /**
     * Gets Android version string
     */
    fun getAndroidVersion(): String {
        return "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }
    
    /**
     * Gets formatted first run timestamp
     */
    fun getFirstRunTime(): String {
        val timestamp = prefsManager.getFirstRunTime()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
    
    /**
     * Gets total launch count
     */
    fun getLaunchCount(): Int {
        return prefsManager.getLaunchCount()
    }
    
    /**
     * Gets total usage time in minutes
     */
    fun getTotalUsageMinutes(): Long {
        return prefsManager.getTotalUsageMinutes()
    }
    
    /**
     * Gets license status string (in Turkish)
     */
    fun getLicenseStatus(): String {
        return if (prefsManager.isLicenseActive()) {
            "Geçerli"
        } else {
            "Geçersiz"
        }
    }
    
    /**
     * Gets license expiry date if active
     */
    fun getLicenseExpiryDate(): String? {
        if (!prefsManager.isLicenseActive()) {
            return null
        }
        
        val expiryTime = prefsManager.getLicenseExpiryTime()
        if (expiryTime == 0L) {
            return null
        }
        
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return dateFormat.format(Date(expiryTime))
    }
    
    /**
     * Generates complete device report for Telegram
     * This is sent automatically on first launch
     */
    fun generateDeviceReport(): String {
        val report = StringBuilder()
        
        report.appendLine("🆕 Yeni Cihaz Kaydı")
        report.appendLine()
        report.appendLine("📱 Cihaz Bilgileri:")
        report.appendLine("━━━━━━━━━━━━━━━━")
        report.appendLine("Cihaz Kimliği: ${generateDeviceFingerprint()}")
        report.appendLine("Marka/Model: ${getDeviceBrandModel()}")
        report.appendLine("Android Sürümü: ${getAndroidVersion()}")
        report.appendLine()
        report.appendLine("📊 Kullanım Bilgileri:")
        report.appendLine("━━━━━━━━━━━━━━━━")
        report.appendLine("İlk Çalıştırma: ${getFirstRunTime()}")
        report.appendLine("Toplam Açılış: ${getLaunchCount()}")
        report.appendLine("Toplam Kullanım: ${getTotalUsageMinutes()} dakika")
        report.appendLine()
        report.appendLine("🔐 Lisans Durumu:")
        report.appendLine("━━━━━━━━━━━━━━━━")
        report.appendLine("Durum: ${getLicenseStatus()}")
        
        val expiryDate = getLicenseExpiryDate()
        if (expiryDate != null) {
            report.appendLine("Bitiş Tarihi: $expiryDate")
        }
        
        report.appendLine()
        report.appendLine("⚠️ Bu cihaz için lisans kodu oluşturmak üzere bu bilgileri kullanın.")
        
        return report.toString()
    }
}
