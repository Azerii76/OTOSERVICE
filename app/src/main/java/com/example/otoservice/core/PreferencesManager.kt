package com.example.otoservice.core

import android.content.Context
import android.content.SharedPreferences

// Data class for selected app info
data class SelectedAppInfo(val packageName: String, val appName: String)

/**
 * Centralized manager for SharedPreferences access
 * Provides type-safe access to all app preferences
 */
class PreferencesManager(private val context: Context) {
    
    private fun getPrefs(name: String): SharedPreferences {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }
    
    // License preferences
    val licensePrefs: SharedPreferences
        get() = getPrefs(Constants.PREFS_LICENSE)
    
    val appSelectionPrefs: SharedPreferences
        get() = getPrefs(Constants.PREFS_APP_SELECTION)
    
    val locationPrefs: SharedPreferences
        get() = getPrefs(Constants.PREFS_LOCATION)
    
    val deviceInfoPrefs: SharedPreferences
        get() = getPrefs(Constants.PREFS_DEVICE_INFO)
    
    // License-related methods
    fun isLicenseActive(): Boolean {
        return licensePrefs.getBoolean(Constants.KEY_LICENSE_ACTIVE, false)
    }
    
    fun getLicenseCode(): String? {
        return licensePrefs.getString(Constants.KEY_LICENSE_CODE, null)
    }
    
    fun getLicenseExpiryTime(): Long {
        return licensePrefs.getLong(Constants.KEY_LICENSE_EXPIRY_TIME, 0L)
    }
    
    fun isTimeTamperLocked(): Boolean {
        return licensePrefs.getBoolean(Constants.KEY_TIME_TAMPER_LOCK, false)
    }
    
    fun isCloneTamperLocked(): Boolean {
        return licensePrefs.getBoolean(Constants.KEY_CLONE_TAMPER_LOCK, false)
    }
    
    fun isCodeTamperLocked(): Boolean {
        return licensePrefs.getBoolean(Constants.KEY_CODE_TAMPER_LOCK, false)
    }
    
    fun isAnyTamperLocked(): Boolean {
        return isTimeTamperLocked() || isCloneTamperLocked() || isCodeTamperLocked()
    }
    
    fun isDeviceInfoSent(): Boolean {
        return licensePrefs.getBoolean(Constants.KEY_DEVICE_INFO_SENT, false)
    }
    
    fun setDeviceInfoSent(sent: Boolean) {
        licensePrefs.edit().putBoolean(Constants.KEY_DEVICE_INFO_SENT, sent).apply()
    }
    
    // App selection methods
    fun getReplyMessage(): String {
        return appSelectionPrefs.getString("reply_message", Constants.DEFAULT_REPLY_MESSAGE) 
            ?: Constants.DEFAULT_REPLY_MESSAGE
    }
    
    fun setReplyMessage(message: String) {
        appSelectionPrefs.edit().putString("reply_message", message).apply()
    }
    
    fun getReplyDelaySeconds(): Int {
        return appSelectionPrefs.getInt("reply_delay_seconds", Constants.DEFAULT_REPLY_DELAY_SECONDS)
    }
    
    fun setReplyDelaySeconds(seconds: Int) {
        appSelectionPrefs.edit().putInt("reply_delay_seconds", seconds).apply()
    }
    
    // Location methods
    fun getSelectedDistricts(): List<String> {
        val json = locationPrefs.getString("selected_districts", null) ?: return emptyList()
        return json.split(",").filter { it.isNotEmpty() }
    }
    
    fun saveSelectedDistricts(districts: List<String>) {
        locationPrefs.edit().putString("selected_districts", districts.joinToString(",")).apply()
    }
    
    fun getDistrictDurations(): List<Int> {
        val json = locationPrefs.getString("district_durations", null) ?: return emptyList()
        return json.split(",").mapNotNull { it.toIntOrNull() }
    }
    
    fun saveDistrictDurations(durations: List<Int>) {
        locationPrefs.edit().putString("district_durations", durations.joinToString(",")).apply()
    }
    
    fun getLocationUpdateInterval(): Int {
        return locationPrefs.getInt("update_interval_seconds", Constants.DEFAULT_LOCATION_UPDATE_INTERVAL_SECONDS)
    }
    
    fun setLocationUpdateInterval(seconds: Int) {
        locationPrefs.edit().putInt("update_interval_seconds", seconds).apply()
    }
    
    // Helper functions with simpler names
    fun saveReplyMessage(message: String) = setReplyMessage(message)
    fun saveReplyDelay(seconds: Int) = setReplyDelaySeconds(seconds)
    fun getReplyDelay() = getReplyDelaySeconds()
    fun saveLocationUpdateInterval(minutes: Int) = setLocationUpdateInterval(minutes * 60)
    
    // Auto-reply service state
    fun isAutoReplyEnabled(): Boolean {
        return appSelectionPrefs.getBoolean("auto_reply_enabled", false)
    }
    
    fun setAutoReplyEnabled(enabled: Boolean) {
        appSelectionPrefs.edit().putBoolean("auto_reply_enabled", enabled).apply()
    }
    
    // App selection helpers - convert between List and Set
    fun getSelectedApps(): List<SelectedAppInfo> {
        val set = appSelectionPrefs.getStringSet("selected_apps", emptySet()) ?: emptySet()
        return set.mapNotNull { encoded ->
            val parts = encoded.split("|")
            if (parts.size == 2) SelectedAppInfo(parts[0], parts[1]) else null
        }
    }
    
    fun saveSelectedApps(apps: List<SelectedAppInfo>) {
        val set = apps.map { "${it.packageName}|${it.appName}" }.toSet()
        appSelectionPrefs.edit().putStringSet("selected_apps", set).apply()
    }
    
    fun isLocationSpoofEnabled(): Boolean {
        return locationPrefs.getBoolean("location_spoof_enabled", false)
    }
    
    fun setLocationSpoofEnabled(enabled: Boolean) {
        locationPrefs.edit().putBoolean("location_spoof_enabled", enabled).apply()
    }
    
    // Device info tracking
    fun getDeviceId(): String {
        var deviceId = deviceInfoPrefs.getString("device_id", null)
        if (deviceId == null) {
            // Generate unique device ID on first run
            deviceId = "DEV-${java.util.UUID.randomUUID().toString().take(12).replace("-", "")}"
            deviceInfoPrefs.edit().putString("device_id", deviceId).apply()
        }
        return deviceId
    }
    
    fun getFirstRunTime(): Long {
        var firstRun = deviceInfoPrefs.getLong("first_run_time", 0L)
        if (firstRun == 0L) {
            firstRun = System.currentTimeMillis()
            deviceInfoPrefs.edit().putLong("first_run_time", firstRun).apply()
        }
        return firstRun
    }
    
    fun incrementLaunchCount() {
        val count = deviceInfoPrefs.getInt("launch_count", 0)
        deviceInfoPrefs.edit().putInt("launch_count", count + 1).apply()
    }
    
    fun getLaunchCount(): Int {
        return deviceInfoPrefs.getInt("launch_count", 0)
    }
    
    fun getTotalUsageMinutes(): Long {
        return deviceInfoPrefs.getLong("total_usage_minutes", 0L)
    }
    
    fun addUsageTime(minutes: Long) {
        val current = getTotalUsageMinutes()
        deviceInfoPrefs.edit().putLong("total_usage_minutes", current + minutes).apply()
    }
    
    /**
     * Clears all sensitive data (used when tampering is detected)
     */
    fun wipeSensitiveData() {
        licensePrefs.edit().clear().apply()
        appSelectionPrefs.edit().clear().apply()
        locationPrefs.edit().clear().apply()
    }
}
