package com.example.otoservice

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.otoservice.core.PreferencesManager
import com.example.otoservice.deviceinfo.TelegramSender
import com.example.otoservice.license.LicenseManager
import com.example.otoservice.security.AntiCloneChecker
import com.example.otoservice.security.AntiTamperChecker
import com.example.otoservice.security.TimeValidator
import com.example.otoservice.utils.PermissionHelper
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var prefsManager: PreferencesManager
    private lateinit var licenseManager: LicenseManager
    private lateinit var tabLayout: TabLayout
    private lateinit var lockOverlay: View
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        prefsManager = PreferencesManager(this)
        licenseManager = LicenseManager(this)
        
        if (licenseManager.isMasterLicense()) {
            setContentView(R.layout.activity_main)
            setupUI()
            return
        }
        
        checkSystemTime()
        
        if (!prefsManager.isLicenseActive()) {
            redirectToLicenseActivation()
            return
        }
        
        if (!performSecurityChecks()) {
            return
        }
        
        setContentView(R.layout.activity_main)
        setupUI()
        
        requestAllPermissions()
        
        checkLicenseStatus()
        
        sendDeviceInfoIfFirstLaunch()
    }
    
    private fun requestAllPermissions() {
        if (!PermissionHelper.hasAllCriticalPermissions(this)) {
            AlertDialog.Builder(this)
                .setTitle("İzinler Gerekli")
                .setMessage("Uygulama düzgün çalışması için şu izinlere ihtiyaç duyar:\n\n" +
                        "• Konum (Her zaman) - Sahte konum için\n" +
                        "• Bildirim - Otomatik yanıt için\n" +
                        "• Pil optimizasyonu - Arka planda çalışma için\n\n" +
                        "Lütfen tüm izinleri verin.")
                .setPositiveButton("İzinleri Ver") { _, _ ->
                    PermissionHelper.checkAndRequestAllPermissions(this)
                }
                .setNegativeButton("Sonra") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(false)
                .show()
        }
        
        PermissionHelper.requestBatteryOptimizationExemption(this)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (!PermissionHelper.hasAllCriticalPermissions(this)) {
            AlertDialog.Builder(this)
                .setTitle("Eksik İzinler")
                .setMessage("Bazı izinler verilmedi. Uygulama düzgün çalışmayabilir.\n\n" +
                        "Ayarlar'dan tüm izinleri verebilirsiniz.")
                .setPositiveButton("Ayarlar'a Git") { _, _ ->
                    PermissionHelper.openAppSettings(this)
                }
                .setNegativeButton("Tamam") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }
    
    private fun checkSystemTime() {
        if (licenseManager.isMasterLicense()) {
            return
        }
        
        lifecycleScope.launch {
            try {
                val timeValidator = TimeValidator(this@MainActivity)
                
                if (!timeValidator.quickTimeCheck()) {
                    showTimeManipulationError()
                    return@launch
                }
                
                if (!timeValidator.isSystemTimeValid()) {
                    showTimeManipulationError()
                } else {
                    timeValidator.saveCurrentRealTime()
                }
            } catch (e: Exception) {
            }
        }
    }
    
    private fun performSecurityChecks(): Boolean {
        if (licenseManager.isMasterLicense()) {
            return true
        }
        
        try {
            val antiTamperChecker = AntiTamperChecker(this)
            val antiCloneChecker = AntiCloneChecker(this)
            
            if (!antiTamperChecker.checkIntegrity() || !antiTamperChecker.secondaryIntegrityCheck()) {
                antiTamperChecker.handleTamperingDetected()
                showTamperLockScreen("code")
                return false
            }
            
            if (!antiCloneChecker.checkForClone()) {
                antiCloneChecker.handleCloneDetected()
                showTamperLockScreen("clone")
                return false
            }
        } catch (e: Exception) {
        }
        
        return true
    }
    
    private fun setupUI() {
        tabLayout = findViewById(R.id.tabLayout)
        lockOverlay = findViewById(R.id.lockOverlay)
        
        tabLayout.addTab(tabLayout.newTab().setText("Uygulamalar"))
        tabLayout.addTab(tabLayout.newTab().setText("Konum"))
        tabLayout.addTab(tabLayout.newTab().setText("Ayarlar"))
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let { switchFragment(it.position) }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        
        switchFragment(0)
    }
    
    private fun switchFragment(position: Int) {
        val fragment = when (position) {
            0 -> AppSelectionFragment()
            1 -> LocationFragment()
            2 -> SettingsFragment()
            else -> AppSelectionFragment()
        }
        
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
    
    private fun checkLicenseStatus() {
        if (licenseManager.isMasterLicense()) {
            return
        }
        
        val state = licenseManager.validateLicense()
        
        when (state) {
            LicenseManager.LockState.Valid -> {
            }
            LicenseManager.LockState.Expired -> {
                showLockOverlay("Lisans süresi dolmuştur.")
            }
            LicenseManager.LockState.TimeTamper -> {
                showLockOverlay("Zaman manipülasyonu tespit edildi!")
            }
            LicenseManager.LockState.CloneTamper -> {
                showTamperLockScreen("clone")
            }
            LicenseManager.LockState.CodeTamper -> {
                showTamperLockScreen("code")
            }
            LicenseManager.LockState.NoLicense -> {
                redirectToLicenseActivation()
            }
        }
    }
    
    private fun showLockOverlay(message: String) {
        lockOverlay.visibility = View.VISIBLE
        AlertDialog.Builder(this)
            .setTitle("Lisans Hatası")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Kapat") { _, _ ->
                finishAffinity()
            }
            .show()
    }
    
    private fun showTamperLockScreen(type: String) {
        val message = when (type) {
            "code" -> "Uygulama kodları ile oynanmış! Bu sürüm artık kullanılamaz."
            "clone" -> "Bu uygulama klonlanmış veya kopyalanmış! Orijinal sürümü kullanın."
            else -> "Güvenlik ihlali tespit edildi!"
        }
        
        AlertDialog.Builder(this)
            .setTitle("Güvenlik Uyarısı")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Kapat") { _, _ ->
                finishAffinity()
            }
            .show()
    }
    
    private fun showTimeManipulationError() {
        AlertDialog.Builder(this)
            .setTitle("Zaman Manipülasyonu")
            .setMessage("Sistem saati manipüle edilmiş! Lütfen saat ayarlarınızı düzeltin.")
            .setCancelable(false)
            .setPositiveButton("Kapat") { _, _ ->
                finishAffinity()
            }
            .show()
    }
    
    private fun redirectToLicenseActivation() {
        val intent = Intent(this, LicenseActivationActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun sendDeviceInfoIfFirstLaunch() {
        lifecycleScope.launch {
            try {
                TelegramSender(this@MainActivity).sendDeviceInfoIfNeeded()
            } catch (e: Exception) {
            }
        }
    }
}
