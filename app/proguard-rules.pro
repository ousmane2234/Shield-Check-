-keep class com.shieldcheck.app.data.model.** { *; }
-keep class com.shieldcheck.app.receiver.** { *; }
-keep class com.shieldcheck.app.service.** { *; }

# Supabase
-keep class io.github.supabase.** { *; }
-keep class io.ktor.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Serialization
-keepclassmembers class * {
    *** *_Serializer(...);
}