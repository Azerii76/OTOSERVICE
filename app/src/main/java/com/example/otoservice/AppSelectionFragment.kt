package com.example.otoservice

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.otoservice.R
import com.example.otoservice.core.PreferencesManager
import com.example.otoservice.core.SelectedAppInfo
import com.example.otoservice.utils.AppFilterHelper

class AppSelectionFragment : Fragment() {
    
    private lateinit var prefsManager: PreferencesManager
    private val selectedApps = mutableListOf<SelectedAppInfo>()
    
    private lateinit var selectedAppsContainer: LinearLayout
    private lateinit var addAppButton: Button
    private lateinit var replyMessageInput: EditText
    private lateinit var delaySeekBar: SeekBar
    private lateinit var delayValueText: TextView
    private lateinit var saveSettingsButton: Button
    private lateinit var startServiceButton: Button
    private lateinit var stopServiceButton: Button
    private lateinit var serviceStatusText: TextView
    
    private var isServiceRunning = false
    
    companion object {
        const val MAX_SELECTED_APPS = 6
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_app_selection, container, false)
        
        prefsManager = PreferencesManager(requireContext())
        
        setupUI(view)
        loadSelectedApps()
        loadSettings()
        
        return view
    }
    
    private fun setupUI(view: View) {
        selectedAppsContainer = view.findViewById(R.id.selectedAppsContainer)
        addAppButton = view.findViewById(R.id.addAppButton)
        replyMessageInput = view.findViewById(R.id.replyMessageInput)
        delaySeekBar = view.findViewById(R.id.delaySeekBar)
        delayValueText = view.findViewById(R.id.delayValueText)
        saveSettingsButton = view.findViewById(R.id.saveSettingsButton)
        startServiceButton = view.findViewById(R.id.startServiceButton)
        stopServiceButton = view.findViewById(R.id.stopServiceButton)
        serviceStatusText = view.findViewById(R.id.serviceStatusText)
        
        addAppButton.setOnClickListener {
            if (selectedApps.size < MAX_SELECTED_APPS) {
                showAppPicker()
            } else {
                Toast.makeText(context, "Maksimum $MAX_SELECTED_APPS uygulama seçebilirsiniz", Toast.LENGTH_SHORT).show()
            }
        }
        
        saveSettingsButton.setOnClickListener {
            saveAllSettings()
        }
        
        startServiceButton.setOnClickListener {
            startAutoReplyService()
        }
        
        stopServiceButton.setOnClickListener {
            stopAutoReplyService()
        }
        
        delaySeekBar.max = 60
        delaySeekBar.progress = 5
        delaySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                delayValueText.text = "${progress}s"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        updateServiceStatus()
    }
    
    private fun loadSelectedApps() {
        selectedApps.clear()
        selectedApps.addAll(prefsManager.getSelectedApps())
        updateSelectedAppsUI()
    }
    
    private fun loadSettings() {
        val message = prefsManager.getReplyMessage()
        val delay = prefsManager.getReplyDelay()
        
        replyMessageInput.setText(message)
        delaySeekBar.progress = delay
        delayValueText.text = "${delay}s"
    }
    
    private fun updateSelectedAppsUI() {
        selectedAppsContainer.removeAllViews()
        
        for (appInfo in selectedApps) {
            val itemView = layoutInflater.inflate(R.layout.item_selected_app, selectedAppsContainer, false)
            
            val appNameText = itemView.findViewById<TextView>(R.id.appNameText)
            val removeButton = itemView.findViewById<Button>(R.id.removeButton)
            
            appNameText.text = appInfo.appName
            
            removeButton.setOnClickListener {
                selectedApps.remove(appInfo)
                updateSelectedAppsUI()
                prefsManager.saveSelectedApps(selectedApps)
                Toast.makeText(context, "${appInfo.appName} kaldırıldı", Toast.LENGTH_SHORT).show()
            }
            
            selectedAppsContainer.addView(itemView)
        }
    }
    
    private fun saveReplyMessage() {
        val message = replyMessageInput.text.toString()
        prefsManager.saveReplyMessage(message)
    }
    
    private fun saveAllSettings() {
        val message = replyMessageInput.text.toString()
        val delay = delaySeekBar.progress
        
        if (message.isEmpty()) {
            Toast.makeText(context, "Lütfen yanıt mesajı yazın", Toast.LENGTH_SHORT).show()
            return
        }
        
        prefsManager.saveReplyMessage(message)
        prefsManager.saveReplyDelay(delay)
        
        Toast.makeText(context, "Ayarlar kaydedildi", Toast.LENGTH_SHORT).show()
    }
    
    private fun startAutoReplyService() {
        if (selectedApps.isEmpty()) {
            Toast.makeText(context, "Lütfen en az bir uygulama seçin", Toast.LENGTH_SHORT).show()
            return
        }
        
        val message = replyMessageInput.text.toString()
        if (message.isEmpty()) {
            Toast.makeText(context, "Lütfen yanıt mesajı yazın", Toast.LENGTH_SHORT).show()
            return
        }
        
        saveAllSettings()
        
        prefsManager.setAutoReplyEnabled(true)
        isServiceRunning = true
        updateServiceStatus()
        
        Toast.makeText(context, "Otomatik yanıt başlatıldı", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopAutoReplyService() {
        prefsManager.setAutoReplyEnabled(false)
        isServiceRunning = false
        updateServiceStatus()
        
        Toast.makeText(context, "Otomatik yanıt durduruldu", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateServiceStatus() {
        isServiceRunning = prefsManager.isAutoReplyEnabled()
        
        if (isServiceRunning) {
            serviceStatusText.text = "Durum: Çalışıyor ✓"
            serviceStatusText.setTextColor(android.graphics.Color.GREEN)
            startServiceButton.isEnabled = false
            stopServiceButton.isEnabled = true
        } else {
            serviceStatusText.text = "Durum: Durduruldu"
            serviceStatusText.setTextColor(android.graphics.Color.RED)
            startServiceButton.isEnabled = true
            stopServiceButton.isEnabled = false
        }
    }
    
    private fun showAppPicker() {
        val userApps = AppFilterHelper.getFilteredUserApps(requireContext())
        
        if (userApps.isEmpty()) {
            Toast.makeText(context, "Mesajlaşma uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        
        val appNames = userApps.map { it.label }.toTypedArray()
        
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Uygulama Seçin (${selectedApps.size}/$MAX_SELECTED_APPS)")
        builder.setItems(appNames) { dialog, which ->
            val selectedApp = userApps[which]
            
            val appInfo = SelectedAppInfo(selectedApp.packageName, selectedApp.label)
            if (!selectedApps.any { it.packageName == selectedApp.packageName }) {
                selectedApps.add(appInfo)
                updateSelectedAppsUI()
                prefsManager.saveSelectedApps(selectedApps)
                Toast.makeText(context, "${selectedApp.label} eklendi", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Bu uygulama zaten ekli", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("İptal") { dialog, _ -> dialog.dismiss() }
        
        try {
            builder.show()
        } catch (e: Exception) {
            Toast.makeText(context, "Uygulama listesi yüklenemedi", Toast.LENGTH_SHORT).show()
        }
    }
}
