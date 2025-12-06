package com.datawatchdog.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

data class InstalledApp(
    val packageName: String,
    val appName: String
)

class InstalledAppsProvider(private val context: Context) {
    fun getAllInstalledApps(): List<InstalledApp> {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        return packages
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 || isCommonApp(it.packageName) }
            .map { appInfo ->
                InstalledApp(
                    packageName = appInfo.packageName,
                    appName = packageManager.getApplicationLabel(appInfo).toString()
                )
            }
            .sortedBy { it.appName }
    }
    
    private fun isCommonApp(packageName: String): Boolean {
        val commonApps = listOf(
            "com.whatsapp", "com.facebook", "com.instagram", "com.twitter",
            "com.google.android.youtube", "com.netflix", "com.spotify.music",
            "com.snapchat.android", "com.telegram.messenger", "com.tiktok",
            "com.chrome.beta", "com.android.chrome", "com.opera.browser"
        )
        return commonApps.any { packageName.contains(it) }
    }
}
