-dontwarn javax.annotation.processing.AbstractProcessor
-dontwarn javax.annotation.processing.SupportedOptions

-keep class com.apps.adrcotfas.goodtime.settings.GoodtimeLauncherAlias
-keep class com.apps.adrcotfas.goodtime.settings.ProductivityLauncherAlias

-keep class * extends androidx.room.RoomDatabase { <init>(); }

# ===== Google API Client - Targeted Rules =====
# Keep fields annotated with @Key (needed for JSON serialization)
-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}

# Keep generic signatures and annotations for reflection
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault

# Google Drive API models
-keep class com.google.api.services.drive.model.** { *; }

# Google HTTP/JSON internals that use reflection
-keepclassmembers class com.google.api.client.http.** { *; }
-keepclassmembers class com.google.api.client.json.** { *; }
-keepclassmembers class com.google.api.client.util.** { *; }

# Google Auth - only the specific credential classes
-keep class com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential { *; }
-keep class com.google.android.gms.auth.api.signin.** { *; }

# ===== Missing classes (not available on Android) =====
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**
-dontwarn org.apache.http.**