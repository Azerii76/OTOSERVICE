package com.example.otoservice.location

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.otoservice.R
import com.example.otoservice.core.PreferencesManager
import com.example.otoservice.license.LicenseManager
import kotlinx.coroutines.*
import kotlin.random.Random

/**
 * Foreground service that spoofs location to Istanbul districts
 * Rotates through selected districts with random movement within each
 */
class LocationSpoofService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var prefsManager: PreferencesManager
    private lateinit var licenseManager: LicenseManager
    private lateinit var locationManager: LocationManager
    
    private var spoofJob: Job? = null
    private val mockProviderName = LocationManager.GPS_PROVIDER
    
    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        prefsManager = PreferencesManager(this)
        licenseManager = LicenseManager(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start foreground with notification
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Start location spoofing
        startLocationSpoof()
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        stopLocationSpoof()
        serviceScope.cancel()
    }
    
    /**
     * Creates the foreground service notification
     */
    private fun createNotification(): Notification {
        createNotificationChannel()
        
        val notificationIntent = Intent(this, Class.forName("com.example.otoservice.MainActivity"))
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OtoService – Sahte Konum Aktif")
            .setContentText("İlçeler arasında sahte konum kullanılıyor.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    /**
     * Creates notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Konum Servisi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Sahte konum servisi bildirimleri"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    /**
     * Starts the location spoofing loop
     */
    private fun startLocationSpoof() {
        spoofJob?.cancel()
        
        spoofJob = serviceScope.launch {
            try {
                // Setup mock provider
                if (!setupMockProvider()) {
                    Log.e(TAG, "Failed to setup mock provider")
                    stopSelf()
                    return@launch
                }
                
                // Get configuration
                val districts = prefsManager.getSelectedDistricts()
                val durations = prefsManager.getDistrictDurations()
                val updateIntervalSeconds = prefsManager.getLocationUpdateInterval()
                
                if (districts.isEmpty()) {
                    Log.e(TAG, "No districts selected")
                    stopSelf()
                    return@launch
                }
                
                // Main spoofing loop
                var currentDistrictIndex = 0
                
                while (isActive) {
                    // Check license status
                    val lockState = licenseManager.checkLicenseStatus()
                    if (lockState != LicenseManager.LockState.Valid) {
                        Log.w(TAG, "License invalid, stopping service")
                        stopSelf()
                        break
                    }
                    
                    // Get current district
                    val districtName = districts[currentDistrictIndex]
                    val districtDuration = durations.getOrElse(currentDistrictIndex) { 20 }
                    val district = IstanbulDistricts.findByName(districtName)
                    
                    if (district == null) {
                        currentDistrictIndex = (currentDistrictIndex + 1) % districts.size
                        continue
                    }
                    
                    Log.d(TAG, "Moving to district: ${district.name} for $districtDuration minutes")
                    
                    // Calculate number of updates for this district
                    val totalUpdates = (districtDuration * 60) / updateIntervalSeconds
                    
                    // Spoof locations within this district
                    repeat(totalUpdates) { updateNumber ->
                        if (!isActive) return@launch
                        
                        // Generate random location within district radius
                        val location = generateRandomLocation(district)
                        
                        // Push mock location
                        pushMockLocation(location)
                        
                        Log.d(TAG, "Update $updateNumber/$totalUpdates - ${district.name}: ${location.latitude}, ${location.longitude}")
                        
                        // Wait for next update
                        delay(updateIntervalSeconds * 1000L)
                    }
                    
                    // Move to next district
                    currentDistrictIndex = (currentDistrictIndex + 1) % districts.size
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in location spoof", e)
                stopSelf()
            }
        }
    }
    
    /**
     * Stops the location spoofing
     */
    private fun stopLocationSpoof() {
        spoofJob?.cancel()
        spoofJob = null
        
        try {
            locationManager.removeTestProvider(mockProviderName)
        } catch (e: Exception) {
            Log.w(TAG, "Error removing mock provider", e)
        }
    }
    
    /**
     * Sets up the mock location provider
     */
    private fun setupMockProvider(): Boolean {
        return try {
            Log.d(TAG, "Setting up mock provider...")
            
            try {
                locationManager.removeTestProvider(mockProviderName)
                Log.d(TAG, "Removed existing mock provider")
            } catch (e: Exception) {
                Log.d(TAG, "No existing mock provider to remove")
            }
            
            locationManager.addTestProvider(
                mockProviderName,
                false, false, false, false,
                true, true, true,
                android.location.Criteria.POWER_LOW,
                android.location.Criteria.ACCURACY_FINE
            )
            Log.d(TAG, "Mock provider added")
            
            locationManager.setTestProviderEnabled(mockProviderName, true)
            Log.d(TAG, "Mock provider enabled")
            
            Log.d(TAG, "Mock provider setup successful ✓")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException - App not set as mock location app in Developer Options", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up mock provider: ${e.message}", e)
            false
        }
    }
            Log.e(TAG, "Error setting up mock provider", e)
            false
        }
    }
    
    /**
     * Generates a random location within a district's radius
     */
    private fun generateRandomLocation(district: IstanbulDistricts.District): Location {
        // Random offset within ~500 meters (approximately ±0.005 degrees)
        val latOffset = (Random.nextDouble() - 0.5) * 0.01
        val lonOffset = (Random.nextDouble() - 0.5) * 0.01
        
        return Location(mockProviderName).apply {
            latitude = district.centerLat + latOffset
            longitude = district.centerLon + lonOffset
            accuracy = 10f + Random.nextFloat() * 20f // 10-30 meters accuracy
            altitude = 0.0
            bearing = Random.nextFloat() * 360f
            speed = Random.nextFloat() * 2f // 0-2 m/s walking speed
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                bearingAccuracyDegrees = 10f
                speedAccuracyMetersPerSecond = 0.5f
                verticalAccuracyMeters = 5f
            }
        }
    }
    
    /**
     * Pushes a mock location to the system
     */
    private fun pushMockLocation(location: Location) {
        try {
            locationManager.setTestProviderLocation(mockProviderName, location)
            Log.d(TAG, "Mock location pushed: lat=${location.latitude}, lon=${location.longitude}, accuracy=${location.accuracy}")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException - Mock location permission denied or app not set as mock location app", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException - Mock provider not found", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing mock location: ${e.message}", e)
        }
    }
    
    companion object {
        private const val TAG = "LocationSpoofService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_spoof_channel"
        
        // Track service running state
        @Volatile
        var isServiceRunning: Boolean = false
            private set
    }
}
