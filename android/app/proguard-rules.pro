# OwnVoice release (R8) keep rules.
#
# AGP references this file from `android/app/build.gradle.kts`; it must exist
# whenever `isMinifyEnabled = true`.

# Keep useful info for crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod

# AndroidX, Jetpack Compose, OkHttp and zxing-android-embedded ship their own
# consumer ProGuard rules, so only app/bridge specifics are listed here.

# The bridge talks to the desktop companion over OkHttp; silence the optional
# transports OkHttp probes for reflectively.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# org.json is part of the Android platform, never bundle/warn about it.
-dontwarn org.json.**

# Kotlin metadata is used by reflection helpers and crash de-obfuscation.
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# Application/Activity entry points and manifest-declared services are kept by
# AGP, but the app class is referenced from the manifest by name only.
-keep class com.example.ownvoice.OwnVoiceApplication { *; }
-keep class com.example.ownvoice.MainActivity { *; }

# Keep @Composable entry points; R8 can otherwise strip methods whose only
# remaining reference was inlined into a lambda.
-keep,allowobfuscation,allowshrinking class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Accessibility/IME helpers are instantiated by the framework by class name.
-keep class com.example.ownvoice.ime.** { *; }
-keep class com.example.ownvoice.overlay.** { *; }
