package com.example.otoservice.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.otoservice.core.PreferencesManager
import com.example.otoservice.license.LicenseManager
import kotlinx.coroutines.*

class AutoReplyAccessibilityService : AccessibilityService() {
    
    private lateinit var prefsManager: PreferencesManager
    private lateinit var licenseManager: LicenseManager
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val processedMessages = mutableSetOf<String>()
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        
        prefsManager = PreferencesManager(this)
        licenseManager = LicenseManager(this)
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            
            notificationTimeout = 100
        }
        
        serviceInfo = info
        
        Log.d(TAG, "AutoReplyAccessibilityService connected")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        if (!prefsManager.isAutoReplyEnabled()) {
            return
        }
        
        val lockState = licenseManager.checkLicenseStatus()
        if (lockState != LicenseManager.LockState.Valid) {
            return
        }
        
        val packageName = event.packageName?.toString() ?: return
        
        val selectedApps = prefsManager.getSelectedApps()
        if (!selectedApps.any { it.packageName == packageName }) {
            return
        }
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                handleNotification(event, packageName)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleWindowChange(event, packageName)
            }
        }
    }
    
    private fun handleNotification(event: AccessibilityEvent, packageName: String) {
        try {
            val notification = event.parcelableData as? Notification ?: return
            
            val extras = notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            
            if (title.isEmpty() && text.isEmpty()) return
            
            val messageKey = "$packageName:$title:${text.take(20)}:${System.currentTimeMillis() / 60000}"
            
            if (processedMessages.contains(messageKey)) {
                Log.d(TAG, "Message already processed: $messageKey")
                return
            }
            
            processedMessages.add(messageKey)
            
            if (processedMessages.size > 100) {
                val toRemove = processedMessages.take(50)
                processedMessages.removeAll(toRemove.toSet())
            }
            
            Log.d(TAG, "New message from $packageName - Title: $title")
            
            scheduleReply(packageName, title, text)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling notification", e)
        }
    }
    
    private fun handleWindowChange(event: AccessibilityEvent, packageName: String) {
        val rootNode = rootInActiveWindow ?: return
        
        val hasMessageField = findNodeByViewId(rootNode, "message_input") != null ||
                             findNodeByViewId(rootNode, "entry") != null ||
                             findNodeByText(rootNode, "Message") != null
        
        if (hasMessageField) {
            Log.d(TAG, "Message screen detected for $packageName")
        }
        
        rootNode.recycle()
    }
    
    private fun scheduleReply(packageName: String, sender: String, message: String) {
        val delaySeconds = prefsManager.getReplyDelaySeconds().toLong()
        
        serviceScope.launch {
            delay(delaySeconds * 1000L)
            
            handler.post {
                sendReply(packageName, sender)
            }
        }
    }
    
    private fun sendReply(packageName: String, sender: String) {
        try {
            val rootNode = rootInActiveWindow ?: return
            
            val replyMessage = prefsManager.getReplyMessage()
            
            val messageField = findNodeByViewId(rootNode, "entry") ?:
                              findNodeByViewId(rootNode, "message_input") ?:
                              findNodeByClassName(rootNode, "android.widget.EditText")
            
            if (messageField != null) {
                val args = android.os.Bundle()
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, replyMessage)
                messageField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                
                delay(500)
                
                val sendButton = findNodeByViewId(rootNode, "send") ?:
                                findNodeByContentDescription(rootNode, "Send") ?:
                                findNodeByText(rootNode, "Send")
                
                if (sendButton != null) {
                    sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.d(TAG, "Reply sent to $sender via $packageName")
                } else {
                    Log.w(TAG, "Send button not found")
                }
                
                messageField.recycle()
                sendButton?.recycle()
            } else {
                Log.w(TAG, "Message field not found")
            }
            
            rootNode.recycle()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending reply", e)
        }
    }
    
    private fun delay(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
    
    private fun findNodeByViewId(node: AccessibilityNodeInfo?, viewId: String): AccessibilityNodeInfo? {
        if (node == null) return null
        
        if (node.viewIdResourceName?.contains(viewId) == true) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByViewId(child, viewId)
            if (result != null) {
                child.recycle()
                return result
            }
            child.recycle()
        }
        
        return null
    }
    
    private fun findNodeByText(node: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        if (node == null) return null
        
        if (node.text?.contains(text, ignoreCase = true) == true) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByText(child, text)
            if (result != null) {
                child.recycle()
                return result
            }
            child.recycle()
        }
        
        return null
    }
    
    private fun findNodeByContentDescription(node: AccessibilityNodeInfo?, desc: String): AccessibilityNodeInfo? {
        if (node == null) return null
        
        if (node.contentDescription?.contains(desc, ignoreCase = true) == true) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByContentDescription(child, desc)
            if (result != null) {
                child.recycle()
                return result
            }
            child.recycle()
        }
        
        return null
    }
    
    private fun findNodeByClassName(node: AccessibilityNodeInfo?, className: String): AccessibilityNodeInfo? {
        if (node == null) return null
        
        if (node.className?.contains(className) == true && node.isEditable) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByClassName(child, className)
            if (result != null) {
                child.recycle()
                return result
            }
            child.recycle()
        }
        
        return null
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "AutoReplyAccessibilityService destroyed")
    }
    
    companion object {
        private const val TAG = "AutoReplyAccessibility"
    }
}
