# Keep Room entities
-keep class com.datawatchdog.db.** { *; }

# Keep ViewModels
-keep class com.datawatchdog.viewmodel.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep Kotlin metadata
-keepclassmembers class ** {
    *** Companion;
}

# Keep data classes
-keep class com.datawatchdog.util.AppDataUsage { *; }
-keep class com.datawatchdog.util.BundleInfo { *; }
