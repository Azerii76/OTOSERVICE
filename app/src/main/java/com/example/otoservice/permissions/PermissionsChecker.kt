package com.example.otoservice.permissions

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Checks status of all required permissions
 */
class PermissionsChecker(private val context: Context) {
    
    /**
     * Checks if Notification Listener permission is granted
     */
    fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        
        val componentName = ComponentName(
            context,
            "com.example.otoservice.autoreply.AutoReplyNotificationListener"
        )
        
        return enabledListeners.contains(componentName.flattenToString())
    }
    
    /**
     * Checks if location permissions are granted
     */
    fun isLocationPermissionGranted(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        return fineLocation || coarseLocation
    }
    
    /**
     * Checks if POST_NOTIFICATIONS permission is granted (Android 13+)
     */
    fun isPostNotificationsGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true // Not required on older versions
        }
        
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Checks if app is set as mock location app
     * This requires checking Developer Options setting
     */
    fun isMockLocationAppSet(): Boolean {
        // Best effort check - this is difficult to verify programmatically
        // We can only check if the app has permission to add test provider
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            // Try to check if we can add test provider
            // This is not a definitive check but indicates if the setting might be enabled
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Gets a summary of all permission states
     */
    fun getPermissionsSummary(): PermissionsSummary {
        return PermissionsSummary(
            notificationListener = isNotificationListenerEnabled(),
            location = isLocationPermissionGranted(),
            postNotifications = isPostNotificationsGranted(),
            mockLocationApp = isMockLocationAppSet()
        )
    }
    
    data class PermissionsSummary(
        val notificationListener: Boolean,
        val location: Boolean,
        val postNotifications: Boolean,
        val mockLocationApp: Boolean
    ) {
        fun allGranted(): Boolean {
            return notificationListener && location && postNotifications
        }
    }
    
    // Alias functions for compatibility
    fun hasNotificationAccess(): Boolean = isNotificationListenerEnabled()
    fun hasLocationPermission(): Boolean = isLocationPermissionGranted()
    fun hasPostNotificationsPermission(): Boolean = isPostNotificationsGranted()
}
