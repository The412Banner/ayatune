# Keep AYANEO AIDL stubs — the binder transactions are looked up by interface
# descriptor name at runtime; obfuscating these would break the bind.
-keep class com.ayaneo.gamewindow.AyaAidlInterface { *; }
-keep class com.ayaneo.gamewindow.AyaAidlInterface$* { *; }
-keep class com.ayaneo.gamewindow.AyaAidlCallback { *; }
-keep class com.ayaneo.gamewindow.AyaAidlCallback$* { *; }

# kotlinx.serialization companion objects are required at runtime
-keepclasseswithmembers class banner.tune.core.ayaneo.* {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class banner.tune.** {
    *** Companion;
}

# Timber log tag inference uses class name in stack traces
-keepattributes SourceFile,LineNumberTable
