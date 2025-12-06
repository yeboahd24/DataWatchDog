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
                val packageName = appInfo.packageName
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                
                // Check if it's a user app (not a system app)
                val isUserApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
                
                // Check if it's an updated system app (like Chrome, YouTube)
                val isUpdatedSystemApp = appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
                
                // Check if the app has a launcher intent (user-facing apps)
                val hasLauncherIntent = packageManager.getLaunchIntentForPackage(packageName) != null
                
                // Popular apps that should always be considered user apps
                val isPopularApp = isPopularUserApp(packageName)
                
                // Simplified logic: if it's any of these, consider it a user app
                val isUserInstalled = isUserApp || isUpdatedSystemApp || isPopularApp || 
                    (hasLauncherIntent && !isDefinitelySystemApp(packageName))
                
                InstalledApp(
                    packageName = packageName,
                    appName = appName,
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
     * Helper function to identify popular user apps that should always be included
     */
    private fun isPopularUserApp(packageName: String): Boolean {
        val popularApps = setOf(
            // Google Apps that are user apps
            "com.google.android.youtube",
            "com.google.android.apps.maps",
            "com.google.android.gm", // Gmail
            "com.google.android.apps.photos",
            "com.google.android.music",
            "com.google.android.apps.docs", // Google Docs
            "com.google.android.googlequicksearchbox", // Google app
            "com.android.chrome",
            
            // Social Media
            "com.facebook.katana", // Facebook
            "com.facebook.orca", // Messenger
            "com.whatsapp",
            "com.instagram.android",
            "com.twitter.android",
            "com.snapchat.android",
            "com.zhiliaoapp.musically", // TikTok
            "com.linkedin.android",
            
            // Entertainment
            "com.spotify.music",
            "com.netflix.mediaclient",
            "com.amazon.mShop.android.shopping", // Amazon
            "com.ubercab", // Uber
            
            // Communication
            "com.skype.raider", // Skype
            "us.zoom.videomeetings", // Zoom
            "com.microsoft.teams",
            "com.discord",
            
            // Banking & Finance
            "com.paypal.android.p2pmobile",
            
            // Games (common ones)
            "com.king.candycrushsaga",
            "com.supercell.clashofclans",
            
            // Productivity
            "com.microsoft.office.word",
            "com.microsoft.office.excel",
            "com.adobe.reader"
        )
        
        return popularApps.contains(packageName)
    }
    
    /**
     * Helper function to identify apps that are definitely system apps
     */
    private fun isDefinitelySystemApp(packageName: String): Boolean {
        val systemPackages = setOf(
            // Core Android system
            "com.android.systemui",
            "com.android.settings",
            "com.android.phone",
            "com.android.dialer",
            "com.android.contacts",
            "com.android.mms",
            "com.android.launcher",
            "com.android.launcher3",
            "com.android.inputmethod",
            
            // Google system services
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.google.android.setupwizard",
            "com.google.android.partnersetup",
            
            // Manufacturer system apps (examples)
            "com.samsung.android.app.spage",
            "com.samsung.android.bixby",
            "com.huawei.appmarket",
            "com.xiaomi.miuisystem"
        )
        
        // System package prefixes that should be excluded
        val systemPrefixes = listOf(
            "android.",
            "com.android.server",
            "com.qualcomm.",
            "com.mediatek."
        )
        
        return systemPackages.contains(packageName) || 
               systemPrefixes.any { packageName.startsWith(it) }
    }

}
