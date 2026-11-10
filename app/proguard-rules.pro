# StudeCo ProGuard / R8 rules

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }

# Supabase / Ktor
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.serialization.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Models (serialization)
-keepclassmembers class com.celmatech.studeco.data.model.** {
    *;
}

# Keep BuildConfig
-keep class com.celmatech.studeco.BuildConfig { *; }

# Security
-keep class androidx.security.crypto.** { *; }
