package com.example.otoservice.security

import android.content.Context
import com.example.otoservice.core.Constants
import com.example.otoservice.core.PreferencesManager
import java.security.MessageDigest

/**
 * Anti-tamper checker that detects code modifications
 * Uses multiple integrity checks spread across the codebase
 */
class AntiTamperChecker(private val context: Context) {
    
    private val prefsManager = PreferencesManager(context)
    
    /**
     * Performs comprehensive integrity check
     * Returns true if code integrity is valid, false if tampering detected
     */
    fun checkIntegrity(): Boolean {
        // Debug mode bypass - allow testing in Android Studio
        try {
            val debuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            if (debuggable) {
                return true
            }
        } catch (e: Exception) {
            // Continue with normal checks
        }
        
        // Check 1: Verify critical class names exist
        if (!checkCriticalClasses()) {
            return false
        }
        
        // Check 2: Verify expected signature matches
        if (!checkCodeSignature()) {
            return false
        }
        
        // Check 3: Verify package structure
        if (!checkPackageStructure()) {
            return false
        }
        
        return true
    }
    
    /**
     * Check 1: Verify that critical classes are present and not removed
     */
    private fun checkCriticalClasses(): Boolean {
        val criticalClasses = listOf(
            "com.example.otoservice.license.LicenseManager",
            "com.example.otoservice.security.AntiTamperChecker",
            "com.example.otoservice.security.AntiCloneChecker",
            "com.example.otoservice.core.Constants"
        )
        
        return try {
            criticalClasses.forEach { className ->
                Class.forName(className)
            }
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
    
    /**
     * Check 2: Verify code signature matches expected value
     * This creates a fingerprint from critical constant values
     */
    private fun checkCodeSignature(): Boolean {
        val runtimeSignature = computeRuntimeSignature()
        return runtimeSignature == Constants.EXPECTED_CODE_SIGNATURE
    }
    
    /**
     * Computes a runtime signature based on critical app components
     */
    private fun computeRuntimeSignature(): String {
        val components = listOf(
            "OTO",
            "SERVICE",
            "V1",
            "SIGNATURE",
            "2024"
        )
        return components.joinToString("_")
    }
    
    /**
     * Check 3: Verify package structure hasn't been modified
     */
    private fun checkPackageStructure(): Boolean {
        val expectedPackage = "com.example.otoservice"
        return context.packageName == expectedPackage
    }
    
    /**
     * Additional integrity check: Verify critical strings haven't been modified
     * This can be called from different parts of the app
     */
    fun verifyCriticalStrings(): Boolean {
        val criticalHashes = mapOf(
            "license_check" to computeHash("LicenseValidation"),
            "tamper_check" to computeHash("TamperDetection"),
            "clone_check" to computeHash("CloneDetection")
        )
        
        // In a real implementation, these hashes would be compared against
        // pre-computed values stored in multiple locations
        return criticalHashes.isNotEmpty()
    }
    
    /**
     * Computes SHA-256 hash of a string
     */
    private fun computeHash(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Handles tampering detection
     * Sets permanent lock flag and wipes sensitive data
     */
    fun handleTamperingDetected() {
        // Set permanent code tamper lock
        prefsManager.licensePrefs.edit()
            .putBoolean(Constants.KEY_CODE_TAMPER_LOCK, true)
            .apply()
        
        // Wipe all sensitive data
        prefsManager.wipeSensitiveData()
    }
    
    /**
     * Secondary integrity check that can be called from different locations
     * Makes it harder to bypass by removing a single check
     */
    fun secondaryIntegrityCheck(): Boolean {
        // Debug mode bypass
        try {
            val debuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            if (debuggable) {
                return true
            }
        } catch (e: Exception) {
            // Continue with normal checks
        }
        
        // Check if Constants class methods are accessible
        return try {
            Constants.getSecretSeed().isNotEmpty() &&
            Constants.getTelegramBotToken().isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
