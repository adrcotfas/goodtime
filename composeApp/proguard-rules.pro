-dontwarn javax.annotation.processing.AbstractProcessor
-dontwarn javax.annotation.processing.SupportedOptions

-keep class com.apps.adrcotfas.goodtime.settings.GoodtimeLauncherAlias
-keep class com.apps.adrcotfas.goodtime.settings.ProductivityLauncherAlias

-keep class * extends androidx.room.RoomDatabase { <init>(); }

# -------------------------------------
# Google Auth & API Client
# -------------------------------------

-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault

-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}

-keep class com.google.api.services.drive.** { *; }

# -------------------------------------
# Credential Manager
# -------------------------------------

-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
    *;
}