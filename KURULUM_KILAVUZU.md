# OtoService - Kurulum ve Kullanım Kılavuzu

## 📦 Kurulum

### Adım 1: Android Studio'yu İndirin
1. [Android Studio](https://developer.android.com/studio) sitesinden en son sürümü indirin
2. Kurulumu tamamlayın

### Adım 2: Projeyi Açın
1. Android Studio'yu başlatın
2. "File" > "Open" menüsünden `OtoService` klasörünü seçin
3. Gradle sync işleminin tamamlanmasını bekleyin (ilk seferde biraz uzun sürebilir)

### Adım 3: Telegram Bot Ayarları (Zaten Yapılandırılmış)
Bot bilgileri kodda gömülü durumda:
- **Bot Token**: `7996610464:AAHMIs2CwF0--eB4_8S4X1-C5b5kZRYNQMs`
- **Chat ID**: `6466581970`

### Adım 4: APK Oluşturma

#### Debug APK (Test için):
```bash
./gradlew assembleDebug
```
APK konumu: `app/build/outputs/apk/debug/app-debug.apk`

#### Release APK (Yayın için):
```bash
./gradlew assembleRelease
```
APK konumu: `app/build/outputs/apk/release/app-release.apk`

**Not**: Release APK imzalama gerektirir. Keystore oluşturmak için:
1. Android Studio'da Build > Generate Signed Bundle/APK
2. Keystore bilgilerini saklayın

## 🚀 İlk Kullanım

### 1. Uygulama İlk Açılış
- Uygulamayı cihaza yükleyin
- İlk açılışta uygulama **otomatik olarak** cihaz bilgilerini Telegram'a gönderir
- **Kullanıcı bundan haberdar olmaz** (tamamen arka planda çalışır)

### 2. Lisans Kodu Alma
Admin olarak Telegram'dan şu bilgileri alacaksınız:
```
🆕 Yeni Cihaz Kaydı

📱 Cihaz Bilgileri:
━━━━━━━━━━━━━━━━
Cihaz Kimliği: DEV-XXXX-YYYY-ZZZZ
Marka/Model: Samsung Galaxy S21
Android Sürümü: 13 (API 33)

📊 Kullanım Bilgileri:
━━━━━━━━━━━━━━━━
İlk Çalıştırma: 28.11.2024 10:30
Toplam Açılış: 1
Toplam Kullanım: 0 dakika

🔐 Lisans Durumu:
━━━━━━━━━━━━━━━━
Durum: Geçersiz
```

### 3. Lisans Kodu Üretme
Bu bilgileri kullanarak lisans kodu üretmeniz gerekiyor. Lisans kodu şu formatda olmalı:
- **Format**: `XXXX-XXXX-XXXX-XXXX`
- **Örnek**: `A1B2-C3D4-E5F6-G7H8`

**Lisans Algoritması** (Python/Bot için):
```python
import hashlib

def generate_license(device_fingerprint, expiry_date):
    """
    Args:
        device_fingerprint: Telegram'dan gelen Cihaz Kimliği (örn: DEV-1234-5678-9012)
        expiry_date: YYYY-MM-DD formatında son kullanma tarihi
    """
    SECRET_SEED = "SECRET_SEED_PLACEHOLDER"  # Android kodundaki ile aynı olmalı
    
    # Combine components
    combined = f"{SECRET_SEED}|{device_fingerprint}|{expiry_date}"
    
    # Double hash
    hash1 = hashlib.sha256(combined.encode()).hexdigest()
    hash2 = hashlib.sha256((hash1 + SECRET_SEED).encode()).hexdigest()
    
    # Extract 16 characters and format
    code16 = hash2[:16].upper()
    license_code = f"{code16[0:4]}-{code16[4:8]}-{code16[8:12]}-{code16[12:16]}"
    
    return license_code

# Örnek kullanım:
device_id = "DEV-1234-5678-9012"  # Telegram'dan gelen
expiry = "2025-12-31"  # 31 Aralık 2025'e kadar geçerli
license = generate_license(device_id, expiry)
print(f"Lisans Kodu: {license}")
```

### 4. Lisans Kodunu Kullanıcıya Verme
- WhatsApp, SMS veya başka bir yolla kullanıcıya lisans kodunu gönderin
- Kullanıcı uygulamada "Lisans" sekmesine girer
- Kodu "Lisans Kodu" alanına yazar
- "Lisansı Doğrula" butonuna basar

## 🔧 Özellik Kurulumu

### İzinlerin Verilmesi
Kullanıcı "İzinler" sekmesinden tüm izinleri vermeli:

#### 1. Bildirim Erişim İzni
- "Ayarları Aç" butonuna bas
- Listeden "OtoService" uygulamasını bul
- İzni aç

#### 2. Konum İzni
- "Ayarları Aç" butonuna bas
- "İzinler" bölümünden "Konum" seçeneğini aç

#### 3. Bildirim Gösterme İzni
- "Ayarları Aç" butonuna bas
- "Bildirimler" izni ver

#### 4. Sahte Konum Uygulaması
- Cihazda "Geliştirici Seçenekleri"ni aç:
  - Ayarlar > Telefon Hakkında > Yazılım Bilgileri
  - "Yapı numarası"na 7 kez dokun
- Geliştirici Seçenekleri > Sahte konum uygulamasını seç
- "OtoService" uygulamasını seç

### Otomatik Yanıt Kurulumu

1. **"Uygulamalar" sekmesine git**

2. **Uygulama Ekle**:
   - "Uygulama Ekle" butonuna bas
   - WhatsApp, Instagram, Telegram gibi uygulamaları seç
   - Maksimum 6 uygulama seçebilirsin

3. **Yanıt Mesajını Özelleştir**:
   ```
   Varsayılan: "Şu anda meşgulüm, daha sonra yazacağım."
   ```
   İstediğin mesajı yaz

4. **Gecikme Süresini Ayarla**:
   - Minimum: 5 saniye
   - Maksimum: 300 saniye (5 dakika)
   - Önerilen: 10-15 saniye

5. **Sistem Otomatik Çalışacak**:
   - Bildirim geldiğinde belirtilen süre sonra otomatik yanıt gönderilir
   - Yeni gönderenlere öncelik verilir

### Konum Sahteleme Kurulumu

1. **"Konum" sekmesine git**

2. **İlçe Seç ve Sırala**:
   - "İlçe Seç ve Sırala" butonuna bas
   - İstediğin İstanbul ilçelerini seç
   - Her ilçe için süre belirle (5-180 dakika)
   - Sıralamayı ayarla

3. **Güncelleme Aralığı**:
   - 15-60 saniye arası seç
   - Önerilen: 20-30 saniye

4. **Başlat**:
   - "Sahte Konumu Aç" switch'ini aç
   - Bildirim çubuğunda "OtoService – Sahte Konum Aktif" görünecek
   - Konum seçilen ilçeler arasında otomatik döngü yapacak

## ⚠️ Önemli Notlar

### Lisans Sistemi
- ✅ Lisans **sadece 1 cihaza** özeldir
- ✅ Klon uygulamalarda **çalışmaz**
- ✅ Cihaz saati değiştirilirse **lisans kilitlenir**
- ✅ Süre dolunca yeni lisans gerekir

### Güvenlik
- 🔒 Kodlar değiştirilirse uygulama **kalıcı kilitlenir**
- 🔒 Klon algılanırsa uygulama **kalıcı kilitlenir**
- 🔒 Bu kilitler **geri alınamaz**, yeni kurulum gerekir

### Kullanım
- 📱 Root gerekmez
- 📱 Her özellik lisans gerektirir
- 📱 İzinler manuel verilmelidir

## 🐛 Sorun Giderme

### "Lisans kodu geçersiz" hatası
**Sebep**: Kod yanlış veya cihaz eşleşmiyor
**Çözüm**: 
- Kodu doğru yazdığından emin ol
- Boşlukları kontrol et
- Admin'den yeni kod iste

### "Tarih/saat oynama tespit edildi"
**Sebep**: Cihaz saati değiştirilmiş
**Çözüm**:
- Cihaz saatini düzelt
- Admin'den yeni lisans iste
- Eski lisans artık çalışmayacak

### Otomatik yanıt çalışmıyor
**Kontroller**:
1. ✓ Bildirim erişim izni verildi mi?
2. ✓ Uygulama seçildi mi?
3. ✓ Lisans geçerli mi?
4. ✓ Seçili uygulamadan bildirim geliyor mu?

### Sahte konum çalışmıyor
**Kontroller**:
1. ✓ Geliştirici seçeneklerinden sahte konum uygulaması olarak seçildi mi?
2. ✓ Konum izni verildi mi?
3. ✓ En az 1 ilçe seçildi mi?
4. ✓ Lisans geçerli mi?

## 📞 Destek

Sorun yaşarsan:
1. README.md dosyasını oku
2. Bu kılavuzu tekrar gözden geçir
3. Telegram üzerinden iletişime geç

---

**Not**: Bu uygulama sadece Android cihazlarda çalışır. iOS için uygun değildir.
