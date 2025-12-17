package com.example.otoservice

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.otoservice.R
import com.example.otoservice.core.PreferencesManager
import com.example.otoservice.deviceinfo.DeviceInfoCollector
import com.example.otoservice.license.LicenseManager
import kotlinx.coroutines.launch

/**
 * License activation screen - shown on first launch
 */
class LicenseActivationActivity : AppCompatActivity() {
    
    private lateinit var prefsManager: PreferencesManager
    private lateinit var licenseManager: LicenseManager
    private lateinit var deviceInfoCollector: DeviceInfoCollector
    
    private lateinit var deviceIdText: TextView
    private lateinit var deviceInfoText: TextView
    private lateinit var copyDeviceIdButton: Button
    private lateinit var licenseCodeInput: EditText
    private lateinit var activateButton: Button
    private lateinit var statusText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license_activation)
        
        prefsManager = PreferencesManager(this)
        licenseManager = LicenseManager(this)
        deviceInfoCollector = DeviceInfoCollector(this)
        
        // Check system time
        checkSystemTime()
        
        setupUI()
        displayDeviceInfo()
    }
    
    private fun checkSystemTime() {
        lifecycleScope.launch {
            val timeValidator = com.example.otoservice.security.TimeValidator(this@LicenseActivationActivity)
            
            // Quick check first
            if (!timeValidator.quickTimeCheck()) {
                showTimeError()
                return@launch
            }
            
            // Full online check
            if (!timeValidator.isSystemTimeValid()) {
                showTimeError()
            } else {
                timeValidator.saveCurrentRealTime()
            }
        }
    }
    
    private fun setupUI() {
        deviceIdText = findViewById(R.id.deviceIdText)
        deviceInfoText = findViewById(R.id.deviceInfoText)
        copyDeviceIdButton = findViewById(R.id.copyDeviceIdButton)
        licenseCodeInput = findViewById(R.id.licenseCodeInput)
        activateButton = findViewById(R.id.activateButton)
        statusText = findViewById(R.id.statusText)
        
        copyDeviceIdButton.setOnClickListener {
            copyDeviceId()
        }
        
        activateButton.setOnClickListener {
            activateLicense()
        }
    }
    
    private fun displayDeviceInfo() {
        val deviceId = deviceInfoCollector.generateDeviceFingerprint()
        val deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        val androidVersion = "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})"
        
        deviceIdText.text = deviceId
        
        val infoText = """
📱 Cihaz Bilgileri:
━━━━━━━━━━━━━━━━
Cihaz Kimliği: $deviceId
Marka/Model: $deviceModel
$androidVersion

⚠️ Bu bilgileri admin'e göndererek lisans alabilirsiniz.
        """.trimIndent()
        
        deviceInfoText.text = infoText
    }
    
    private fun copyDeviceId() {
        val deviceId = deviceInfoCollector.generateDeviceFingerprint()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Cihaz Kimliği", deviceId)
        clipboard.setPrimaryClip(clip)
        
        Toast.makeText(this, "Cihaz kimliği kopyalandı!", Toast.LENGTH_SHORT).show()
    }
    
    private fun activateLicense() {
        val licenseCode = licenseCodeInput.text.toString().trim()
        
        if (licenseCode.isEmpty()) {
            statusText.text = "❌ Lütfen lisans kodunu girin"
            statusText.visibility = View.VISIBLE
            return
        }
        
        activateButton.isEnabled = false
        activateButton.text = "Doğrulanıyor..."
        statusText.visibility = View.GONE
        
        // Validate license
        val result = licenseManager.validateAndActivateLicense(licenseCode)
        
        when (result) {
            is LicenseManager.LicenseResult.Success -> {
                // License activated successfully
                Toast.makeText(this, "✅ Lisans başarıyla etkinleştirildi!", Toast.LENGTH_LONG).show()
                
                // Move to permissions screen
                val intent = Intent(this, PermissionsSetupActivity::class.java)
                startActivity(intent)
                finish()
            }
            is LicenseManager.LicenseResult.Error -> {
                statusText.text = "❌ ${result.message}"
                statusText.visibility = View.VISIBLE
                activateButton.isEnabled = true
                activateButton.text = "Lisansı Etkinleştir"
            }
        }
    }
    
    private fun showTimeError() {
        android.app.AlertDialog.Builder(this)
            .setTitle("⏰ Saat Manipülasyonu")
            .setMessage("⛔ Sistem saati manipüle edilmiş!\n\nCihaz saatini doğru ayarlayın.\n\nUygulama güvenlik nedeniyle kapatılıyor.")
            .setCancelable(false)
            .setPositiveButton("Tamam") { _, _ ->
                finishAffinity()
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(0)
            }
            .show()
    }
}
