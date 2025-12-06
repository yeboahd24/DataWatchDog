package com.datawatchdog.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val isUserInstalled: Boolean = false
)

class InstalledAppsProvider(private val context: Context) {
    fun getAllInstalledApps(): List<InstalledApp> {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        val allApps = packages.mapNotNull { appInfo ->
            try {
                val isUserApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
                val isUpdatedSystemApp = appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
                val isLaunchable = packageManager.getLaunchIntentForPackage(appInfo.packageName) != null
                
                // Enhanced logic to better identify user-installed apps
                val isUserInstalled = isUserApp || isUpdatedSystemApp || 
                    (isLaunchable && !isSystemPackage(appInfo.packageName))
                
                InstalledApp(
                    packageName = appInfo.packageName,
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    isUserInstalled = isUserInstalled
                )
            } catch (e: Exception) {
                null
            }
        }.distinctBy { it.packageName }
        
        // Sort: user apps first, then by name
        return allApps.sortedWith(
            compareByDescending<InstalledApp> { it.isUserInstalled }
                .thenBy { it.appName.lowercase() }
        )
    }
    
    /**
     * Helper function to identify system packages that shouldn't be considered user apps
     * even if they have launch intents
     */
    private fun isSystemPackage(packageName: String): Boolean {
        val systemPackagePrefixes = listOf(
            "com.android.",
            "com.google.android.",
            "android.",
            "com.samsung.",
            "com.qualcomm.",
            "com.mediatek."
        )
        
        // Exclude these common system packages
        val excludedPackages = setOf(
            "com.android.settings",
            "com.android.systemui",
            "com.android.phone",
            "com.android.contacts",
            "com.android.calendar",
            "com.android.camera",
            "com.android.gallery3d"
        )
        
        // Popular user apps that might start with system prefixes but should be included
        val popularApps = setOf(
            "com.google.android.youtube",
            "com.google.android.apps.maps",
            "com.google.android.gm",
            "com.google.android.apps.photos",
            "com.google.android.music",
            "com.google.android.apps.docs",
            "com.android.chrome"
        )
        
        if (popularApps.contains(packageName)) {
            return false // These are user apps despite system prefix
        }
        
        if (excludedPackages.contains(packageName)) {
            return true
        }
        
        return systemPackagePrefixes.any { packageName.startsWith(it) }
    }

}
