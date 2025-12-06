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
        
        val apps = packages.mapNotNull { appInfo ->
            try {
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                val hasLaunchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName) != null
                
                // Mark as user app if: not system, OR updated system app, OR has launcher
                val isUserApp = !isSystemApp || isUpdatedSystemApp || hasLaunchIntent
                
                InstalledApp(
                    packageName = appInfo.packageName,
                    appName = appInfo.loadLabel(packageManager).toString(),
                    isUserInstalled = isUserApp
                )
            } catch (e: Exception) {
                null
            }
        }
        
        return apps.sortedWith(
            compareByDescending<InstalledApp> { it.isUserInstalled }
                .thenBy { it.appName.lowercase() }
        )
    }
}
