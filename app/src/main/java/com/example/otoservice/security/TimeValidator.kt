package com.example.otoservice.security

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

class TimeValidator(private val context: Context) {
    
    companion object {
        private const val TAG = "TimeValidator"
        private const val MAX_TIME_DIFF_MS = 5 * 60 * 1000L
        
        private val TIME_SERVERS = listOf(
            "https://www.google.com",
            "https://www.cloudflare.com",
            "https://www.microsoft.com"
        )
    }
    
    suspend fun isSystemTimeValid(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val realTime = getRealTimeFromServer()
                
                if (realTime == null) {
                    Log.w(TAG, "Could not verify system time - no internet connection")
                    return@withContext true
                }
                
                val systemTime = System.currentTimeMillis()
                val diff = abs(systemTime - realTime)
                
                if (diff > MAX_TIME_DIFF_MS) {
                    Log.e(TAG, "Time manipulation detected! Diff: ${diff}ms")
                    return@withContext false
                }
                
                Log.d(TAG, "System time is valid. Diff: ${diff}ms")
                return@withContext true
                
            } catch (e: Exception) {
                Log.e(TAG, "Error checking system time", e)
                return@withContext true
            }
        }
    }
    
    private fun getRealTimeFromServer(): Long? {
        for (serverUrl in TIME_SERVERS) {
            try {
                val url = URL(serverUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                
                connection.connect()
                
                val dateHeader = connection.getHeaderField("Date")
                
                if (dateHeader != null) {
                    val serverTime = parseHttpDate(dateHeader)
                    if (serverTime != null) {
                        Log.d(TAG, "Got real time from $serverUrl")
                        return serverTime
                    }
                }
                
                connection.disconnect()
                
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get time from $serverUrl: ${e.message}")
                continue
            }
        }
        
        return null
    }
    
    private fun parseHttpDate(dateStr: String): Long? {
        return try {
            val format = java.text.SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss zzz",
                java.util.Locale.US
            )
            format.timeZone = java.util.TimeZone.getTimeZone("GMT")
            val date = format.parse(dateStr)
            date?.time
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse date: $dateStr", e)
            null
        }
    }
    
    fun quickTimeCheck(): Boolean {
        val prefs = context.getSharedPreferences("time_validator", Context.MODE_PRIVATE)
        val lastRealTime = prefs.getLong("last_real_time", 0L)
        val lastCheckTime = prefs.getLong("last_check_time", 0L)
        
        if (lastRealTime == 0L || lastCheckTime == 0L) {
            return true
        }
        
        val currentSystemTime = System.currentTimeMillis()
        val expectedMinimumTime = lastRealTime
        
        if (currentSystemTime < expectedMinimumTime - MAX_TIME_DIFF_MS) {
            Log.e(TAG, "Time went backwards! Current: $currentSystemTime, Expected: $expectedMinimumTime")
            return false
        }
        
        return true
    }
    
    suspend fun saveCurrentRealTime() {
        withContext(Dispatchers.IO) {
            try {
                val realTime = getRealTimeFromServer()
                realTime?.let {
                    val prefs = context.getSharedPreferences("time_validator", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putLong("last_real_time", it)
                        .putLong("last_check_time", System.currentTimeMillis())
                        .apply()
                    Log.d(TAG, "Saved real time: $it")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving real time", e)
            }
        }
    }
}
