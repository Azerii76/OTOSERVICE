package com.example.otoservice.license

import android.content.Context
import android.os.SystemClock
import com.example.otoservice.core.Constants
import com.example.otoservice.core.PreferencesManager
import com.example.otoservice.deviceinfo.DeviceInfoCollector
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class LicenseManager(private val context: Context) {
    
    private val prefsManager = PreferencesManager(context)
    private val deviceInfoCollector = DeviceInfoCollector(context)
    
    companion object {
        private const val MASTER_PREFIX = "MASTER"
    }
    
    sealed class LicenseResult {
        object Success : LicenseResult()
        data class Error(val message: String) : LicenseResult()
    }
    
    fun isMasterLicense(): Boolean {
        val savedCode = prefsManager.licensePrefs.getString("license_code", "") ?: ""
        return savedCode.startsWith(MASTER_PREFIX)
    }
    
    fun validateAndActivateLicense(inputCode: String): LicenseResult {
        val normalizedCode = normalizeCode(inputCode)
        
        if (normalizedCode.startsWith(MASTER_PREFIX)) {
            return validateMasterLicense(normalizedCode)
        }
        
        if (prefsManager.isCloneTamperLocked()) {
            return LicenseResult.Error("Bu uygulama klon, kopya veya desteklenmeyen bir ortamda çalıştırılamaz.")
        }
        
        if (prefsManager.isCodeTamperLocked()) {
            return LicenseResult.Error("Uygulama kodları ile oynandığı tespit edildi. Bu sürüm artık kullanılamaz.")
        }
        
        if (!isValidFormat(normalizedCode)) {
            return LicenseResult.Error("Lisans kodu geçersiz format.")
        }
        
        val deviceId = deviceInfoCollector.getUniqueDeviceId()
        if (!validateLicenseForDevice(normalizedCode, deviceId)) {
            return LicenseResult.Error("Bu lisans kodu bu cihaz için geçerli değil.")
        }
        
        val expiryTime = System.currentTimeMillis() + (365L * 24L * 60L * 60L * 1000L)
        
        activateNormalLicense(normalizedCode, expiryTime)
        return LicenseResult.Success
    }
    
    private fun validateMasterLicense(code: String): LicenseResult {
        val parts = code.split("-")
        
        if (parts.size < 4) {
            return LicenseResult.Error("Master lisans formatı geçersiz.")
        }
        
        val prefix = parts[0]
        val dateStr = parts[1]
        val signature = parts[2]
        
        if (prefix != MASTER_PREFIX) {
            return LicenseResult.Error("Geçersiz master lisans.")
        }
        
        val expiryTime = if (dateStr == "UNLIMITED") {
            Long.MAX_VALUE
        } else {
            try {
                val format = SimpleDateFormat("yyyyMMdd", Locale.US)
                val date = format.parse(dateStr)
                date?.time ?: return LicenseResult.Error("Geçersiz tarih formatı.")
            } catch (e: Exception) {
                return LicenseResult.Error("Geçersiz tarih formatı.")
            }
        }
        
        if (expiryTime != Long.MAX_VALUE && System.currentTimeMillis() > expiryTime) {
            return LicenseResult.Error("Master lisans süresi dolmuş.")
        }
        
        val expectedSignature = generateMasterSignature(dateStr)
        if (signature != expectedSignature) {
            return LicenseResult.Error("Master lisans imzası geçersiz.")
        }
        
        activateMasterLicense(code, expiryTime)
        return LicenseResult.Success
    }
    
    private fun generateMasterSignature(dateStr: String): String {
        val secret = "OtoServiceMaster2025SecretKey"
        val data = "$MASTER_PREFIX-$dateStr-$secret"
        val bytes = data.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02X".format(it) }.substring(0, 8)
    }
    
    private fun activateMasterLicense(code: String, expiryTime: Long) {
        prefsManager.licensePrefs.edit().apply {
            putString("license_code", code)
            putBoolean(Constants.KEY_LICENSE_ACTIVE, true)
            putLong(Constants.KEY_LICENSE_EXPIRY, expiryTime)
            putBoolean("is_master_license", true)
            putLong(Constants.KEY_LICENSE_ACTIVATION_TIME, System.currentTimeMillis())
            putBoolean(Constants.KEY_CLONE_TAMPER_LOCK, false)
            putBoolean(Constants.KEY_CODE_TAMPER_LOCK, false)
            putBoolean(Constants.KEY_TIME_TAMPER_LOCK, false)
            apply()
        }
    }
    
    private fun activateNormalLicense(code: String, expiryTime: Long) {
        prefsManager.licensePrefs.edit().apply {
            putString("license_code", code)
            putBoolean(Constants.KEY_LICENSE_ACTIVE, true)
            putLong(Constants.KEY_LICENSE_EXPIRY, expiryTime)
            putBoolean("is_master_license", false)
            putLong(Constants.KEY_LICENSE_ACTIVATION_TIME, System.currentTimeMillis())
            apply()
        }
        updateTimeTracking()
    }
    
    fun validateLicense(): LockState {
        if (isMasterLicense()) {
            val expiryTime = prefsManager.getLicenseExpiryTime()
            if (expiryTime != Long.MAX_VALUE && System.currentTimeMillis() > expiryTime) {
                return LockState.Expired
            }
            return LockState.Valid
        }
        
        if (prefsManager.isCloneTamperLocked()) {
            return LockState.CloneTamper
        }
        
        if (prefsManager.isCodeTamperLocked()) {
            return LockState.CodeTamper
        }
        
        if (!prefsManager.isLicenseActive()) {
            return LockState.NoLicense
        }
        
        if (detectTimeTamper()) {
            prefsManager.licensePrefs.edit()
                .putBoolean(Constants.KEY_TIME_TAMPER_LOCK, true)
                .apply()
            return LockState.TimeTamper
        }
        
        updateTimeTracking()
        
        val nowWall = System.currentTimeMillis()
        val expiryTime = prefsManager.getLicenseExpiryTime()
        
        if (nowWall > expiryTime) {
            prefsManager.licensePrefs.edit()
                .putBoolean(Constants.KEY_LICENSE_ACTIVE, false)
                .apply()
            return LockState.Expired
        }
        
        return LockState.Valid
    }
    
    fun checkLicenseStatus(): LockState {
        if (isMasterLicense()) {
            val expiryTime = prefsManager.getLicenseExpiryTime()
            if (expiryTime != Long.MAX_VALUE && System.currentTimeMillis() > expiryTime) {
                return LockState.Expired
            }
            return LockState.Valid
        }
        
        if (prefsManager.isCloneTamperLocked()) {
            return LockState.CloneTamper
        }
        
        if (prefsManager.isCodeTamperLocked()) {
            return LockState.CodeTamper
        }
        
        if (!prefsManager.isLicenseActive()) {
            return LockState.NoLicense
        }
        
        if (checkTimeManipulation()) {
            return LockState.TimeTamper
        }
        
        val expiryTime = prefsManager.getLicenseExpiryTime()
        val currentTime = System.currentTimeMillis()
        
        if (currentTime > expiryTime) {
            return LockState.Expired
        }
        
        return LockState.Valid
    }
    
    private fun detectTimeTamper(): Boolean {
        if (isMasterLicense()) {
            return false
        }
        
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        
        val lastWall = prefsManager.licensePrefs.getLong(Constants.KEY_LAST_KNOWN_WALL_TIME, 0L)
        val lastElapsed = prefsManager.licensePrefs.getLong(Constants.KEY_LAST_KNOWN_ELAPSED_TIME, 0L)
        
        if (nowWall + Constants.BACKWARD_THRESHOLD_MS < lastWall) {
            return true
        }
        
        if (nowWall > lastWall + Constants.MAX_FORWARD_JUMP_MS) {
            return true
        }
        
        if (lastElapsed > 0 && nowElapsed + Constants.BACKWARD_THRESHOLD_MS < lastElapsed) {
            return true
        }
        
        return false
    }
    
    private fun checkTimeManipulation(): Boolean = detectTimeTamper()
    
    private fun updateTimeTracking() {
        if (isMasterLicense()) {
            return
        }
        
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        
        prefsManager.licensePrefs.edit().apply {
            putLong(Constants.KEY_LAST_KNOWN_WALL_TIME, nowWall)
            putLong(Constants.KEY_LAST_KNOWN_ELAPSED_TIME, nowElapsed)
            apply()
        }
    }
    
    private fun normalizeCode(code: String): String {
        return code.trim().replace(" ", "").replace("\n", "").uppercase()
    }
    
    private fun isValidFormat(code: String): Boolean {
        if (code.startsWith(MASTER_PREFIX)) {
            return true
        }
        return code.length in 18..30
    }
    
    private fun extractExpiryDate(code: String): String? {
        if (code.length < 8) return null
        return code.substring(0, 8)
    }
    
    private fun parseDate(dateStr: String): Date? {
        return try {
            val format = SimpleDateFormat("yyyyMMdd", Locale.US)
            format.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun validateLicenseForDevice(code: String, deviceId: String): Boolean {
        val parts = code.split("-")
        if (parts.isEmpty()) return false
        val codeDeviceHash = parts[0]
        val expectedHash = generateDeviceHash(deviceId).substring(0, 8)
        return codeDeviceHash == expectedHash
    }
    
    private fun generateDeviceHash(deviceId: String): String {
        val bytes = deviceId.toByteArray()
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02X".format(it) }
    }
    
    enum class LockState {
        Valid,
        NoLicense,
        Expired,
        TimeTamper,
        CloneTamper,
        CodeTamper
    }
}
