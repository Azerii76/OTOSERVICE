package com.example.otoservice

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.otoservice.core.PreferencesManager
import com.example.otoservice.deviceinfo.DeviceInfoCollector
import com.example.otoservice.license.LicenseManager
import com.example.otoservice.permissions.PermissionsChecker
import java.text.SimpleDateFormat
import java.util.*

class SettingsFragment : Fragment() {
    
    private lateinit var prefsManager: PreferencesManager
    private lateinit var licenseManager: LicenseManager
    private lateinit var deviceInfoCollector: DeviceInfoCollector
    private lateinit var permissionsChecker: PermissionsChecker
    
    private lateinit var licenseStatusText: TextView
    private lateinit var licenseCodeText: TextView
    private lateinit var licenseExpiryText: TextView
    private lateinit var deviceIdText: TextView
    private lateinit var deviceModelText: TextView
    private lateinit var copyDeviceIdButton: Button
    private lateinit var permissionsContainer: LinearLayout
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        
        prefsManager = PreferencesManager(requireContext())
        licenseManager = LicenseManager(requireContext())
        deviceInfoCollector = DeviceInfoCollector(requireContext())
        permissionsChecker = PermissionsChecker(requireContext())
        
        initializeViews(view)
        loadLicenseInfo()
        loadDeviceInfo()
        
        return view
    }
    
    override fun onResume() {
        super.onResume()
        updatePermissionsStatus()
    }
    
    private fun initializeViews(view: View) {
        licenseStatusText = view.findViewById(R.id.licenseStatusText)
        licenseCodeText = view.findViewById(R.id.licenseCodeText)
        licenseExpiryText = view.findViewById(R.id.licenseExpiryText)
        deviceIdText = view.findViewById(R.id.deviceIdText)
        deviceModelText = view.findViewById(R.id.deviceModelText)
        copyDeviceIdButton = view.findViewById(R.id.copyDeviceIdButton)
        permissionsContainer = view.findViewById(R.id.permissionsContainer)
        
        copyDeviceIdButton.setOnClickListener {
            copyDeviceId()
        }
    }
    
    private fun loadLicenseInfo() {
        val licenseCode = prefsManager.licensePrefs.getString("license_code", "N/A") ?: "N/A"
        val expiryTime = prefsManager.getLicenseExpiryTime()
        
        val status = licenseManager.checkLicenseStatus()
        when (status) {
            LicenseManager.LockState.Valid -> {
                licenseStatusText.text = "✅ Aktif"
                licenseStatusText.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
            }
            LicenseManager.LockState.Expired -> {
                licenseStatusText.text = "❌ Süresi Dolmuş"
                licenseStatusText.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
            }
            else -> {
                licenseStatusText.text = "⚠️ Geçersiz"
                licenseStatusText.setTextColor(requireContext().getColor(android.R.color.holo_orange_dark))
            }
        }
        
        licenseCodeText.text = licenseCode
        
        if (expiryTime > 0) {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            licenseExpiryText.text = dateFormat.format(Date(expiryTime))
        } else {
            licenseExpiryText.text = "Bilinmiyor"
        }
    }
    
    private fun loadDeviceInfo() {
        val deviceId = deviceInfoCollector.generateDeviceFingerprint()
        val deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        
        deviceIdText.text = deviceId
        deviceModelText.text = deviceModel
        
        updatePermissionsStatus()
    }
    
    private fun copyDeviceId() {
        val deviceId = deviceInfoCollector.generateDeviceFingerprint()
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Cihaz Kimliği", deviceId)
        clipboard.setPrimaryClip(clip)
        
        Toast.makeText(context, "Cihaz kimliği kopyalandı!", Toast.LENGTH_SHORT).show()
    }
    
    private fun updatePermissionsStatus() {
        permissionsContainer.removeAllViews()
        
        val permissions = listOf(
            Triple("Bildirim Erişimi", permissionsChecker.hasNotificationAccess(), 
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")),
            Triple("Konum İzni", permissionsChecker.hasLocationPermission(),
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)),
            Triple("Bildirim Gönderme", permissionsChecker.hasPostNotificationsPermission(),
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                }),
            Triple("Sahte Konum", permissionsChecker.isMockLocationAppSet(),
                Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        )
        
        for ((title, granted, intent) in permissions) {
            val itemView = layoutInflater.inflate(R.layout.item_permission_simple, permissionsContainer, false)
            
            val titleText = itemView.findViewById<TextView>(R.id.permissionTitle)
            val statusText = itemView.findViewById<TextView>(R.id.permissionStatus)
            val settingsButton = itemView.findViewById<Button>(R.id.settingsButton)
            
            titleText.text = title
            
            if (granted) {
                statusText.text = "✅"
                statusText.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
                settingsButton.visibility = View.GONE
            } else {
                statusText.text = "❌"
                statusText.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
                settingsButton.visibility = View.VISIBLE
                settingsButton.setOnClickListener {
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Ayarlar açılamadı", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            
            permissionsContainer.addView(itemView)
        }
    }
}
