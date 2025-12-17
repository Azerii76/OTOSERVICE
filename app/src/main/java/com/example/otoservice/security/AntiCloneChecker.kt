package com.example.otoservice.security

import android.content.Context
import android.os.Build
import android.os.Process
import android.os.UserManager
import com.example.otoservice.core.Constants
import com.example.otoservice.core.PreferencesManager
import java.io.File

/**
 * Anti-clone checker that detects if app is running in:
 * - Cloned/dual app environment
 * - Parallel space
 * - Modified package name
 * - Secondary user profile
 */
class AntiCloneChecker(private val context: Context) {
    
    private val prefsManager = PreferencesManager(context)
    
    /**
     * Performs comprehensive clone detection
     * Returns true if app is in original environment, false if clone detected
     */
    fun checkForClone(): Boolean {
        // Debug mode bypass - allow testing in Android Studio
        try {
            val debuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            if (debuggable) {
                return true
            }
        } catch (e: Exception) {
            // Continue with normal checks
        }
        
        // Check 1: Verify package name hasn't been changed
        if (!checkPackageName()) {
            return false
        }
        
        // Check 2: Detect dual app / parallel space patterns
        if (detectDualAppEnvironment()) {
            return false
        }
        
        // Check 3: Check if running in secondary user profile
        if (detectSecondaryUserProfile()) {
            return false
        }
        
        // Check 4: Detect process name manipulation
        if (detectProcessNameManipulation()) {
            return false
        }
        
        return true
    }
    
    /**
     * Check 1: Verify package name matches expected value
     */
    private fun checkPackageName(): Boolean {
        val expectedPackage = "com.example.otoservice"
        val actualPackage = context.packageName
        return actualPackage == expectedPackage
    }
    
    /**
     * Check 2: Detect if app is running in dual app / parallel space environment
     */
    private fun detectDualAppEnvironment(): Boolean {
        // Check app label
        val appLabel = getAppLabel()
        if (containsCloneKeywords(appLabel)) {
            return true
        }
        
        // Check data directory path
        val dataDir = context.applicationInfo.dataDir
        if (containsCloneKeywords(dataDir)) {
            return true
        }
        
        // Check native library directory
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        if (containsCloneKeywords(nativeLibDir)) {
            return true
        }
        
        // Check for multiple data directory patterns
        if (detectMultipleDataDirPattern(dataDir)) {
            return true
        }
        
        return false
    }
    
    /**
     * Check 3: Detect if running in secondary user profile
     */
    private fun detectSecondaryUserProfile(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
                val userHandle = Process.myUserHandle()
                
                // Check if this is not the primary user (user 0)
                if (userManager != null) {
                    val serialNumber = userManager.getSerialNumberForUser(userHandle)
                    // Primary user typically has serial number 0
                    return serialNumber != 0L
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check 4: Detect if process name has been manipulated
     */
    private fun detectProcessNameManipulation(): Boolean {
        val processName = getProcessName()
        if (processName.isNullOrEmpty()) {
            return false
        }
        
        // Process name should match package name in normal environment
        val expectedProcessName = context.packageName
        if (processName != expectedProcessName) {
            // Check if it contains clone-related keywords
            if (containsCloneKeywords(processName)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Gets the current app label
     */
    private fun getAppLabel(): String {
        return try {
            val pm = context.packageManager
            val appInfo = context.applicationInfo
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Gets the current process name
     */
    private fun getProcessName(): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Process.myProcessName()
            } else {
                // Fallback for older versions
                val pid = Process.myPid()
                val file = File("/proc/$pid/cmdline")
                if (file.exists()) {
                    file.readText().trim('\u0000')
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Checks if a string contains clone-related keywords
     */
    private fun containsCloneKeywords(text: String?): Boolean {
        if (text == null) return false
        
        val cloneKeywords = listOf(
            "clone",
            "dual",
            "parallel",
            "multi",
            "virtual",
            "sandbox",
            "island",
            "shelter",
            "profile",
            "copy",
            "replica",
            "999", // Some clone apps use numeric suffixes
            ":p", // Some parallel spaces use process suffixes
            "_64" // Some cloners use architecture suffixes
        )
        
        val lowerText = text.lowercase()
        return cloneKeywords.any { keyword -> lowerText.contains(keyword) }
    }
    
    /**
     * Detects if data directory follows a clone app pattern
     * Example: /data/user/999/... or /data/data/.../clone/...
     */
    private fun detectMultipleDataDirPattern(dataDir: String): Boolean {
        // Check for non-standard user IDs (primary user is typically 0)
        val userPattern = Regex("/data/user/(\\d+)/")
        val match = userPattern.find(dataDir)
        if (match != null) {
            val userId = match.groupValues[1].toIntOrNull() ?: 0
            if (userId > 10) { // User IDs > 10 are often clone/secondary profiles
                return true
            }
        }
        
        return false
    }
    
    /**
     * Handles clone detection
     * Sets permanent lock flag and wipes sensitive data
     */
    fun handleCloneDetected() {
        // Set permanent clone tamper lock
        prefsManager.licensePrefs.edit()
            .putBoolean(Constants.KEY_CLONE_TAMPER_LOCK, true)
            .apply()
        
        // Wipe all sensitive data
        prefsManager.wipeSensitiveData()
    }
}
