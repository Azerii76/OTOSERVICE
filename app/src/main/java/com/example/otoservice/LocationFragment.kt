package com.example.otoservice

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.otoservice.core.PreferencesManager
import com.example.otoservice.location.IstanbulDistricts
import com.example.otoservice.location.LocationSpoofService

class LocationFragment : Fragment() {
    
    private lateinit var prefsManager: PreferencesManager
    private var selectedDistricts = mutableListOf<String>()
    
    private lateinit var selectedDistrictsText: TextView
    private lateinit var selectDistrictButton: Button
    private lateinit var updateIntervalSeekBar: SeekBar
    private lateinit var intervalValueText: TextView
    private lateinit var startServiceButton: Button
    private lateinit var stopServiceButton: Button
    private lateinit var statusText: TextView
    
    companion object {
        const val MAX_SELECTED_DISTRICTS = 5
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_location, container, false)
        
        prefsManager = PreferencesManager(requireContext())
        
        initializeViews(view)
        setupListeners()
        loadSettings()
        updateServiceStatus()
        
        return view
    }
    
    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }
    
    private fun initializeViews(view: View) {
        selectedDistrictsText = view.findViewById(R.id.selectedDistrictsText)
        selectDistrictButton = view.findViewById(R.id.selectDistrictButton)
        updateIntervalSeekBar = view.findViewById(R.id.updateIntervalSeekBar)
        intervalValueText = view.findViewById(R.id.intervalValueText)
        startServiceButton = view.findViewById(R.id.startServiceButton)
        stopServiceButton = view.findViewById(R.id.stopServiceButton)
        statusText = view.findViewById(R.id.statusText)
    }
    
    private fun setupListeners() {
        selectDistrictButton.setOnClickListener {
            showDistrictPicker()
        }
        
        updateIntervalSeekBar.max = 30
        updateIntervalSeekBar.progress = 5
        updateIntervalSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val minutes = if (progress < 1) 1 else progress
                intervalValueText.text = "$minutes dakika"
                prefsManager.saveLocationUpdateInterval(minutes)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        startServiceButton.setOnClickListener {
            if (selectedDistricts.isEmpty()) {
                Toast.makeText(context, "Lütfen en az bir ilçe seçin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val intent = Intent(requireContext(), LocationSpoofService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(intent)
            } else {
                requireContext().startService(intent)
            }
            
            Toast.makeText(context, "Konum servisi başlatıldı", Toast.LENGTH_SHORT).show()
            updateServiceStatus()
        }
        
        stopServiceButton.setOnClickListener {
            val intent = Intent(requireContext(), LocationSpoofService::class.java)
            requireContext().stopService(intent)
            
            Toast.makeText(context, "Konum servisi durduruldu", Toast.LENGTH_SHORT).show()
            updateServiceStatus()
        }
    }
    
    private fun loadSettings() {
        selectedDistricts = prefsManager.getSelectedDistricts().toMutableList()
        val interval = prefsManager.getLocationUpdateInterval()
        
        updateSelectedDistrictsUI()
        updateIntervalSeekBar.progress = interval / 60
        intervalValueText.text = "${interval / 60} dakika"
    }
    
    private fun updateSelectedDistrictsUI() {
        selectedDistrictsText.text = if (selectedDistricts.isEmpty()) {
            "Henüz ilçe seçilmedi"
        } else {
            selectedDistricts.joinToString(", ")
        }
    }
    
    private fun updateServiceStatus() {
        val isRunning = LocationSpoofService.isServiceRunning
        
        if (isRunning) {
            statusText.text = "✅ Konum servisi aktif"
            statusText.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            startServiceButton.isEnabled = false
            stopServiceButton.isEnabled = true
        } else {
            statusText.text = "❌ Konum servisi pasif"
            statusText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            startServiceButton.isEnabled = true
            stopServiceButton.isEnabled = false
        }
    }
    
    private fun showDistrictPicker() {
        val districtNames = IstanbulDistricts.getDistrictNames().sorted().toTypedArray()
        val checkedItems = BooleanArray(districtNames.size) { index ->
            selectedDistricts.contains(districtNames[index])
        }
        
        val tempSelectedDistricts = selectedDistricts.toMutableList()
        
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("İlçe Seçin (Maksimum $MAX_SELECTED_DISTRICTS)")
        
        builder.setMultiChoiceItems(
            districtNames,
            checkedItems
        ) { dialog, which, isChecked ->
            val district = districtNames[which]
            
            if (isChecked) {
                if (tempSelectedDistricts.size < MAX_SELECTED_DISTRICTS) {
                    if (!tempSelectedDistricts.contains(district)) {
                        tempSelectedDistricts.add(district)
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Maksimum $MAX_SELECTED_DISTRICTS ilçe seçebilirsiniz",
                        Toast.LENGTH_SHORT
                    ).show()
                    (dialog as AlertDialog).listView.setItemChecked(which, false)
                }
            } else {
                tempSelectedDistricts.remove(district)
            }
        }
        
        builder.setPositiveButton("Tamam") { dialog, _ ->
            selectedDistricts.clear()
            selectedDistricts.addAll(tempSelectedDistricts)
            prefsManager.saveSelectedDistricts(selectedDistricts)
            updateSelectedDistrictsUI()
            Toast.makeText(
                context,
                "${selectedDistricts.size} ilçe seçildi",
                Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()
        }
        
        builder.setNegativeButton("İptal") { dialog, _ ->
            dialog.dismiss()
        }
        
        builder.setNeutralButton("Tümünü Temizle") { dialog, _ ->
            selectedDistricts.clear()
            prefsManager.saveSelectedDistricts(selectedDistricts)
            updateSelectedDistrictsUI()
            Toast.makeText(
                context,
                "Tüm seçimler temizlendi",
                Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()
        }
        
        builder.show()
    }
}
