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
            .mapNotNull { appInfo ->
                try {
                    InstalledApp(
                        packageName = appInfo.packageName,
                        appName = packageManager.getApplicationLabel(appInfo).toString()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }
    
    private fun isCommonApp(packageName: String): Boolean {
        val commonApps = listOf(
            "whatsapp", "facebook", "instagram", "twitter", "messenger",
            "youtube", "netflix", "spotify", "snapchat", "telegram",
            "tiktok", "chrome", "opera", "firefox", "brave",
            "gmail", "maps", "drive", "photos", "calendar"
        )
        return commonApps.any { packageName.lowercase().contains(it) }
    }
}
