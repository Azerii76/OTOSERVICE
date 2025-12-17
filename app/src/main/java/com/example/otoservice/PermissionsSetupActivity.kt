package com.example.otoservice

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.otoservice.permissions.PermissionsChecker

class PermissionsSetupActivity : AppCompatActivity() {
    
    private lateinit var permissionsChecker: PermissionsChecker
    private lateinit var permissionsContainer: LinearLayout
    private lateinit var continueButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions_setup)
        
        permissionsChecker = PermissionsChecker(this)
        
        initializeViews()
        checkPermissions()
    }
    
    override fun onResume() {
        super.onResume()
        checkPermissions()
    }
    
    private fun initializeViews() {
        permissionsContainer = findViewById(R.id.permissionsContainer)
        continueButton = findViewById(R.id.continueButton)
        
        continueButton.setOnClickListener {
            if (allPermissionsGranted()) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                val notGranted = getNotGrantedPermissions()
                Toast.makeText(
                    this,
                    "Lütfen tüm izinleri verin: $notGranted",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun checkPermissions() {
        permissionsContainer.removeAllViews()
        
        val permissions = listOf(
            PermissionItem(
                "Bildirim Erişimi",
                "Otomatik yanıt için gerekli",
                permissionsChecker.hasNotificationAccess(),
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            ),
            PermissionItem(
                "Konum İzni",
                "Konum sahtekarlığı için gerekli",
                permissionsChecker.hasLocationPermission(),
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            ),
            PermissionItem(
                "Bildirim Gönderme",
                "Uygulama bildirimleri için gerekli",
                permissionsChecker.hasPostNotificationsPermission(),
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
            ),
            PermissionItem(
                "Sahte Konum Uygulaması",
                "Konum sahtelemesi için gerekli",
                permissionsChecker.isMockLocationAppSet(),
                Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            )
        )
        
        for (permission in permissions) {
            val view = layoutInflater.inflate(R.layout.item_permission, permissionsContainer, false)
            
            val titleText = view.findViewById<TextView>(R.id.permissionTitle)
            val descText = view.findViewById<TextView>(R.id.permissionDescription)
            val statusText = view.findViewById<TextView>(R.id.permissionStatus)
            val settingsButton = view.findViewById<Button>(R.id.permissionSettingsButton)
            
            titleText.text = permission.title
            descText.text = permission.description
            
            if (permission.granted) {
                statusText.text = "✅ Verildi"
                statusText.setTextColor(getColor(android.R.color.holo_green_dark))
                settingsButton.visibility = android.view.View.GONE
            } else {
                statusText.text = "❌ Verilmedi"
                statusText.setTextColor(getColor(android.R.color.holo_red_dark))
                settingsButton.visibility = android.view.View.VISIBLE
                settingsButton.setOnClickListener {
                    try {
                        startActivity(permission.settingsIntent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Ayarlar açılamadı", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            
            permissionsContainer.addView(view)
        }
        
        updateContinueButton()
    }
    
    private fun updateContinueButton() {
        if (allPermissionsGranted()) {
            continueButton.isEnabled = true
            continueButton.text = "Devam Et"
        } else {
            continueButton.isEnabled = false
            continueButton.text = "Tüm İzinleri Verin"
        }
    }
    
    private fun allPermissionsGranted(): Boolean {
        return permissionsChecker.hasNotificationAccess() &&
                permissionsChecker.hasLocationPermission() &&
                permissionsChecker.hasPostNotificationsPermission() &&
                permissionsChecker.isMockLocationAppSet()
    }
    
    private fun getNotGrantedPermissions(): String {
        val notGranted = mutableListOf<String>()
        if (!permissionsChecker.hasNotificationAccess()) notGranted.add("Bildirim Erişimi")
        if (!permissionsChecker.hasLocationPermission()) notGranted.add("Konum")
        if (!permissionsChecker.hasPostNotificationsPermission()) notGranted.add("Bildirim Gönderme")
        if (!permissionsChecker.isMockLocationAppSet()) notGranted.add("Sahte Konum")
        return notGranted.joinToString(", ")
    }
    
    data class PermissionItem(
        val title: String,
        val description: String,
        val granted: Boolean,
        val settingsIntent: Intent
    )
}
