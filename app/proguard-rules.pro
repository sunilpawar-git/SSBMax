# SSBMax ProGuard Rules
# Production-grade code optimization and obfuscation

# ================================
# Debug Information (for Crashlytics)
# ================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations for better crash reports
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

# Keep signature for generics
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ================================
# Firebase & Google Services
# ================================

# Firebase Crashlytics
-keepattributes *Annotation*
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Firebase Firestore
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }
-dontwarn com.google.firebase.auth.**

# Firebase Storage
-keep class com.google.firebase.storage.** { *; }
-dontwarn com.google.firebase.storage.**

# Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Google Play Billing
-keep class com.android.billingclient.** { *; }

# ================================
# Kotlin & Coroutines
# ================================

# Kotlin Metadata
-keep class kotlin.Metadata { *; }
-keep class kotlin.** { *; }
-dontwarn kotlin.**

# Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Kotlin serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** {
    <methods>;
}

# ================================
# Jetpack Compose
# ================================

# Keep Compose runtime classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Composable functions
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep Compose UI
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.material.** { *; }

# ================================
# Hilt & Dagger
# ================================

# Keep Hilt generated code
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# Keep Hilt modules
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.Module class * { *; }
-keep @dagger.hilt.components.SingletonComponent class * { *; }

# Keep Hilt entry points
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# Keep injected members
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}

-keepclasseswithmembers class * {
    @javax.inject.Inject <fields>;
}

-keepclasseswithmembers class * {
    @javax.inject.Inject <methods>;
}

# ================================
# Android Architecture Components
# ================================

# ViewModel
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class androidx.lifecycle.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Keep Room DAOs
-keepclassmembers class * {
    @androidx.room.Query <methods>;
}

# Navigation
-keep class androidx.navigation.** { *; }

# ================================
# Domain Models (for Firebase)
# ================================

# Keep all domain model classes for Firebase serialization
-keep class com.ssbmax.core.domain.model.** { *; }
-keepclassmembers class com.ssbmax.core.domain.model.** {
    *;
}

# Keep data classes
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ================================
# Coil Image Loading
# ================================
-keep class coil.** { *; }
-keep class coil3.** { *; }
-dontwarn coil.**
-dontwarn coil3.**

# ================================
# OkHttp & Retrofit (if used)
# ================================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ================================
# General Android
# ================================

# Keep Android Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ================================
# SSBMax Specific Rules
# ================================

# Keep test submission models
-keep class com.ssbmax.core.domain.model.test.** { *; }

# Keep user models
-keep class com.ssbmax.core.domain.model.user.** { *; }

# Keep subscription models
-keep class com.ssbmax.core.domain.model.subscription.** { *; }

# Keep repository interfaces (for Hilt)
-keep interface com.ssbmax.core.domain.repository.** { *; }

# Keep use case classes
-keep class com.ssbmax.core.domain.usecase.** { *; }

# ================================
# Optimization
# ================================

# Aggressive optimization
-optimizationpasses 5
-allowaccessmodification

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ================================
# Warnings to Suppress
# ================================
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**