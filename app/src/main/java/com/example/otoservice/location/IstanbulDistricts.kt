package com.example.otoservice.location

/**
 * Istanbul districts data with coordinates
 * Contains name, center latitude, and center longitude for each district
 */
object IstanbulDistricts {
    
    data class District(
        val name: String,
        val centerLat: Double,
        val centerLon: Double
    )
    
    /**
     * List of major Istanbul districts with their approximate center coordinates
     */
    val districts = listOf(
        District("Esenyurt", 41.0302, 28.6745),
        District("Bağcılar", 41.0394, 28.8575),
        District("Avcılar", 40.9789, 28.7206),
        District("Kadıköy", 40.9833, 29.0333),
        District("Beşiktaş", 41.0422, 29.0078),
        District("Şişli", 41.0602, 28.9870),
        District("Üsküdar", 41.0214, 29.0200),
        District("Bakırköy", 40.9833, 28.8667),
        District("Fatih", 41.0192, 28.9497),
        District("Beyoğlu", 41.0370, 28.9784),
        District("Ataşehir", 40.9833, 29.1167),
        District("Maltepe", 40.9333, 29.1333),
        District("Kartal", 40.9000, 29.1833),
        District("Pendik", 40.8833, 29.2333),
        District("Başakşehir", 41.0817, 28.8094),
        District("Küçükçekmece", 41.0167, 28.7833),
        District("Bahçelievler", 41.0000, 28.8500),
        District("Esenler", 41.0458, 28.8796),
        District("Gaziosmanpaşa", 41.0667, 28.9167),
        District("Sultanbeyli", 40.9667, 29.2667),
        District("Sarıyer", 41.1667, 29.0500),
        District("Kağıthane", 41.0833, 28.9833),
        District("Bayrampaşa", 41.0453, 28.9094),
        District("Zeytinburnu", 40.9897, 28.9014),
        District("Eyüpsultan", 41.0500, 28.9167)
    )
    
    /**
     * Finds a district by name
     */
    fun findByName(name: String): District? {
        return districts.find { it.name.equals(name, ignoreCase = true) }
    }
    
    /**
     * Gets district names list
     */
    fun getDistrictNames(): List<String> {
        return districts.map { it.name }
    }
}
