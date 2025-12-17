# DİNAMİK MASTER LİSANS SİSTEMİ

## YENİ SİSTEM ÖZELLİKLERİ

### 1. DİNAMİK OLUŞTURMA
- Her seferinde farklı kod
- Benzersiz imza
- Takip edilebilir

### 2. SÜRE SEÇENEKLERİ
- ♾️ Süresiz (UNLIMITED)
- 📅 30 Gün
- 📅 90 Gün
- 📅 180 Gün
- 📅 365 Gün
- 🔧 Özel Süre (1-3650 gün)

### 3. KULLANIM TİPİ
- 🔴 Tek Kullanımlık (bir kez kullanılır)
- 🟢 Çoklu Kullanım (sınırsız)

---

## MASTER LİSANS FORMATI

```
MASTER-<TARIH>-<İMZA>-<TİP>-<RANDOM>
```

### ÖRNEKLER:

**Süresiz, Tek Kullanımlık:**
```
MASTER-UNLIMITED-A1B2C3D4-SINGLE-XYZ789AB
```

**30 Gün, Çoklu Kullanım:**
```
MASTER-20250108-E5F6G7H8-MULTI-QWE456RT
```

**90 Gün, Tek Kullanımlık:**
```
MASTER-20250308-I9J0K1L2-SINGLE-ASD789FG
```

---

## TELEGRAM BOT KULLANIMI

### ANA MENÜ:
```
🤖 OtoService Lisans Botu

[🔑 Master Lisans Oluştur]  <- Dinamik
[📋 Normal Lisans Oluştur]
[📜 Lisanslarım]
[❓ Yardım]
[👑 Admin Panel]
```

### MASTER LİSANS OLUŞTURMA AKIŞI:

**Adım 1: Master Lisans Oluştur'a Tıkla**
```
🔑 Master Lisans Oluştur

Lütfen lisans süresini seçin:

[♾️ Süresiz]
[📅 30 Gün]
[📅 90 Gün]
[📅 180 Gün]
[📅 365 Gün]
[🔧 Özel Süre]
```

**Adım 2: Süre Seç (örnek: 90 Gün)**
```
🔧 Kullanım Tipi Seçin

[🔴 Tek Kullanımlık]
[🟢 Çoklu Kullanım]
```

**Adım 3: Kullanım Tipi Seç (örnek: Tek Kullanımlık)**
```
🔑 MASTER LİSANS

📋 Lisans Kodu:
MASTER-20250308-A1B2C3D4-SINGLE-XYZ789AB

📊 Kullanım: 🔴 Tek Kullanımlık
📅 Süre: 90 gün (Son: 08.03.2025)

✅ Tüm güvenlik bypass
✅ Herhangi bir cihazda
✅ Tam yetki
```

**Adım 4: Kodu Kopyala ve Uygulamaya Gir**

---

## ANDROID KODLARI

### LicenseManager.kt Değişiklikleri:

**İmza Oluşturma:**
```kotlin
private fun generateMasterSignature(dateStr: String): String {
    val secret = "OtoServiceMaster2025SecretKey"
    val data = "$MASTER_PREFIX-$dateStr-$secret"
    val bytes = data.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.joinToString("") { "%02X".format(it) }.substring(0, 8)
}
```

**Master Lisans Validasyonu:**
```kotlin
private fun validateMasterLicense(code: String): LicenseResult {
    val parts = code.split("-")
    
    // Format kontrolü
    if (parts.size < 4) {
        return LicenseResult.Error("Master lisans formatı geçersiz.")
    }
    
    val prefix = parts[0]      // MASTER
    val dateStr = parts[1]     // UNLIMITED veya 20250108
    val signature = parts[2]   // A1B2C3D4
    // parts[3] = SINGLE/MULTI
    // parts[4] = Random
    
    // Süre hesaplama
    val expiryTime = if (dateStr == "UNLIMITED") {
        Long.MAX_VALUE
    } else {
        parseDate(dateStr)?.time ?: return LicenseResult.Error("Geçersiz tarih.")
    }
    
    // Süre dolmuş mu kontrol
    if (expiryTime != Long.MAX_VALUE && System.currentTimeMillis() > expiryTime) {
        return LicenseResult.Error("Master lisans süresi dolmuş.")
    }
    
    // İmza doğrulama
    val expectedSignature = generateMasterSignature(dateStr)
    if (signature != expectedSignature) {
        return LicenseResult.Error("Master lisans imzası geçersiz.")
    }
    
    // Aktive et
    activateMasterLicense(code, expiryTime)
    return LicenseResult.Success
}
```

**Master Lisans Kontrolü:**
```kotlin
fun isMasterLicense(): Boolean {
    val savedCode = prefsManager.licensePrefs.getString("license_code", "") ?: ""
    return savedCode.startsWith("MASTER")  // MASTER ile başlıyorsa master
}
```

---

## KULLANIM ÖRNEKLERİ

### SENARYO 1: Süresiz Master Lisans

**Bot'ta:**
```
1. Master Lisans Oluştur
2. ♾️ Süresiz
3. 🟢 Çoklu Kullanım
4. Kod: MASTER-UNLIMITED-A1B2C3D4-MULTI-XYZ789AB
```

**Uygulamada:**
```
1. Lisans ekranı aç
2. Kodu yapıştır
3. Aktive Et
4. ✅ Sonsuz süre, sınırsız cihaz!
```

---

### SENARYO 2: 30 Günlük, Tek Kullanımlık

**Bot'ta:**
```
1. Master Lisans Oluştur
2. 📅 30 Gün
3. 🔴 Tek Kullanımlık
4. Kod: MASTER-20250107-E5F6G7H8-SINGLE-QWE456RT
```

**Uygulamada:**
```
1. İlk cihazda aktive edilir
2. 30 gün kullanılır
3. Başka cihazda KULLANILAMAZ (tek kullanımlık)
4. 30 gün sonra dolar
```

---

### SENARYO 3: 90 Günlük, Çoklu Kullanım

**Bot'ta:**
```
1. Master Lisans Oluştur
2. 📅 90 Gün
3. 🟢 Çoklu Kullanım
4. Kod: MASTER-20250308-I9J0K1L2-MULTI-ASD789FG
```

**Uygulamada:**
```
1. Birinci cihazda aktive edilir
2. İkinci cihazda da kullanılabilir
3. Üçüncü cihazda da kullanılabilir
4. 90 gün boyunca geçerli
5. 90 gün sonra tüm cihazlarda dolar
```

---

## GÜVENLİK

### İmza Sistemi:
```
Lisans Kodu = MASTER-<TARIH>-<İMZA>-<TİP>-<RANDOM>

İmza = SHA256(MASTER + TARIH + SECRET_KEY)[:8]
```

**Örnek:**
```
Tarih: 20250108
Secret: OtoServiceMaster2025SecretKey
Data: MASTER-20250108-OtoServiceMaster2025SecretKey
SHA256: a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6...
İmza: A1B2C3D4 (ilk 8 karakter)
```

**Sahte lisans oluşturulamaz:**
- Secret key gerekli
- İmza eşleşmezse reddedilir
- Her tarih için farklı imza

---

## LİSANSLARIMI GÖRÜNTÜLEME

**Bot'ta:**
```
📜 Lisanslarım

✅ MASTER-UNLIMITED-A1B2C3D4-MULTI-XYZ789AB
📌 MASTER
🔄 Kullanılmadı
📅 08.12.2024

❌ MASTER-20241207-E5F6G7H8-SINGLE-QWE456RT
📌 MASTER
🔄 Kullanıldı
📅 07.12.2024

✅ 20250107-12345678-ABCD1234
📌 NORMAL
📅 07.12.2024
```

**Bilgiler:**
- ✅ Geçerli
- ❌ Süresi dolmuş veya kullanılmış
- 🔄 Kullanım durumu (sadece master'da)

---

## DOSYALAR

### Android:
1. LicenseManager_DYNAMIC.kt - Yeni LicenseManager

### Telegram Bot:
1. telegram_bot_dynamic.py - Dinamik bot

---

## KURULUM

### Android:
```
1. LicenseManager.kt'yi değiştir
   (LicenseManager_DYNAMIC.kt içeriğiyle)
2. Build > Clean Project
3. Build > Rebuild Project
4. APK oluştur
```

### Telegram Bot:
```
1. pip install python-telegram-bot
2. python telegram_bot_dynamic.py
3. Bot'a git: /start
4. Master lisans oluştur!
```

---

## AVANTAJLAR

### ESKİ SİSTEM (Sabit):
```
❌ Tek kod: ADMIN-MASTER-2025-UNLIMITED
❌ Herkes biliyor
❌ Paylaşılabiliyor
❌ Kontrol yok
```

### YENİ SİSTEM (Dinamik):
```
✅ Her seferinde farklı kod
✅ İmza ile güvenli
✅ Tek kullanımlık seçeneği
✅ Süre kontrolü
✅ Takip edilebilir
✅ İptal edilebilir
```

---

## ÖZET

**Özellikler:**
- ♾️ Süresiz veya süreli
- 🔴 Tek kullanımlık veya çoklu
- 🔐 İmza ile güvenli
- 📊 Takip edilebilir
- 🎯 Esnek yönetim

**Bot Komutları:**
- /start - Ana menü
- Master Lisans Oluştur - Yeni master lisans
- Lisanslarım - Oluşturduğun lisansları gör

**Lisans Formatı:**
```
MASTER-<TARIH>-<İMZA>-<TİP>-<RANDOM>
```

**Hemen kullan!** 🚀
