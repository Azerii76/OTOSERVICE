# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep security checks
-keep class com.example.otoservice.security.** { *; }
-keep class com.example.otoservice.license.** { *; }

# Keep notification listener
-keep class com.example.otoservice.autoreply.AutoReplyNotificationListener { *; }

# Keep location service
-keep class com.example.otoservice.location.LocationSpoofService { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

-dontwarn java.lang.instrument.ClassFileTransformer
