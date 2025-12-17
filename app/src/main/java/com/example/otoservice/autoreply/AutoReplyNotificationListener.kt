package com.example.otoservice.autoreply

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.otoservice.core.PreferencesManager
import com.example.otoservice.license.LicenseManager
import kotlinx.coroutines.*

/**
 * NotificationListenerService that monitors notifications from selected apps
 * and sends automatic replies
 */
class AutoReplyNotificationListener : NotificationListenerService() {
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var prefsManager: PreferencesManager
    private lateinit var licenseManager: LicenseManager
    private lateinit var replyQueue: AutoReplyQueue
    
    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager(this)
        licenseManager = LicenseManager(this)
        replyQueue = AutoReplyQueue(this, prefsManager)
        
        Log.d(TAG, "AutoReplyNotificationListener created")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "AutoReplyNotificationListener destroyed")
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        if (sbn == null) return
        
        if (!prefsManager.isAutoReplyEnabled()) {
            return
        }
        
        val lockState = licenseManager.checkLicenseStatus()
        if (lockState != LicenseManager.LockState.Valid) {
            return
        }
        
        val selectedApps = prefsManager.getSelectedApps()
        val selectedPackages = selectedApps.map { it.packageName }
        if (!selectedPackages.contains(sbn.packageName)) {
            return
        }
        
        processNotification(sbn)
    }
    
    /**
     * Processes an incoming notification
     */
    private fun processNotification(sbn: StatusBarNotification) {
        try {
            val notification = sbn.notification ?: return
            
            // Extract message details
            val extras = notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            val sender = title ?: "Unknown"
            val messageText = text ?: ""
            
            // Find reply action
            val replyAction = findReplyAction(notification)
            if (replyAction == null) {
                Log.d(TAG, "No reply action found for ${sbn.packageName}")
                return
            }
            
            // Create notification data
            val notificationData = NotificationData(
                packageName = sbn.packageName,
                sender = sender,
                messageText = messageText,
                replyAction = replyAction,
                timestamp = System.currentTimeMillis()
            )
            
            // Add to reply queue
            replyQueue.enqueue(notificationData)
            
            Log.d(TAG, "Notification enqueued from ${sbn.packageName}: $sender")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification", e)
        }
    }
    
    /**
     * Finds the reply action in a notification
     */
    private fun findReplyAction(notification: Notification): Notification.Action? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH) {
            return null
        }
        
        val actions = notification.actions ?: return null
        
        for (action in actions) {
            if (action.remoteInputs != null && action.remoteInputs.isNotEmpty()) {
                // This action has remote input (reply capability)
                return action
            }
        }
        
        return null
    }
    
    /**
     * Data class for notification information
     */
    data class NotificationData(
        val packageName: String,
        val sender: String,
        val messageText: String,
        val replyAction: Notification.Action,
        val timestamp: Long
    )
    
    companion object {
        private const val TAG = "AutoReplyListener"
    }
}

/**
 * Queue manager for auto-reply
 * Handles prioritization and delayed sending
 */
class AutoReplyQueue(
    private val context: android.content.Context,
    private val prefsManager: PreferencesManager
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val processedSenders = mutableSetOf<String>()
    private val replyJobs = mutableMapOf<String, Job>()
    
    /**
     * Enqueues a notification for auto-reply
     */
    fun enqueue(data: AutoReplyNotificationListener.NotificationData) {
        // Check if we've already replied to this sender recently
        val senderKey = "${data.packageName}:${data.sender}"
        
        if (processedSenders.contains(senderKey)) {
            // Already replied to this sender, lower priority
            Log.d(TAG, "Sender $senderKey already processed, delaying")
            scheduleReply(data, isPriority = false)
        } else {
            // New sender, high priority
            Log.d(TAG, "New sender $senderKey, priority reply")
            scheduleReply(data, isPriority = true)
            processedSenders.add(senderKey)
        }
    }
    
    /**
     * Schedules a reply with appropriate delay
     */
    private fun scheduleReply(data: AutoReplyNotificationListener.NotificationData, isPriority: Boolean) {
        val senderKey = "${data.packageName}:${data.sender}"
        
        // Cancel existing job for this sender if any
        replyJobs[senderKey]?.cancel()
        
        // Get delay from settings
        val baseDelaySeconds = prefsManager.getReplyDelaySeconds()
        
        // Priority senders get base delay, others get extra delay
        val delaySeconds = if (isPriority) {
            baseDelaySeconds
        } else {
            baseDelaySeconds + 10 // Add 10 seconds for non-priority
        }
        
        // Schedule reply
        val job = scope.launch {
            try {
                // Wait for delay
                delay(delaySeconds * 1000L)
                
                // Send reply
                sendReply(data)
                
            } catch (e: CancellationException) {
                Log.d(TAG, "Reply cancelled for $senderKey")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending reply", e)
            } finally {
                replyJobs.remove(senderKey)
            }
        }
        
        replyJobs[senderKey] = job
    }
    
    /**
     * Sends the auto-reply
     */
    private fun sendReply(data: AutoReplyNotificationListener.NotificationData) {
        try {
            val replyMessage = prefsManager.getReplyMessage()
            
            // Get RemoteInput from action
            val remoteInputs = data.replyAction.remoteInputs
            if (remoteInputs == null || remoteInputs.isEmpty()) {
                Log.w(TAG, "No remote inputs available")
                return
            }
            
            val remoteInput = remoteInputs[0]
            
            // Create reply intent
            val replyIntent = Intent()
            val bundle = android.os.Bundle()
            bundle.putCharSequence(remoteInput.resultKey, replyMessage)
            RemoteInput.addResultsToIntent(remoteInputs, replyIntent, bundle)
            
            // Send reply
            data.replyAction.actionIntent.send(context, 0, replyIntent)
            
            Log.d(TAG, "Reply sent to ${data.sender}: $replyMessage")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending reply", e)
        }
    }
    
    /**
     * Clears the processed senders cache
     * Call this periodically or on app restart
     */
    fun clearProcessedSenders() {
        processedSenders.clear()
    }
    
    companion object {
        private const val TAG = "AutoReplyQueue"
    }
}
