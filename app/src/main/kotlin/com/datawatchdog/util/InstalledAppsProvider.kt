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
                
                InstalledApp(
                    packageName = appInfo.packageName,
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    isUserInstalled = isUserApp || isUpdatedSystemApp || isLaunchable
                )
            } catch (e: Exception) {
                null
            }
        }.distinctBy { it.packageName }
        
        // Sort: launchable/user apps first, then by name
        return allApps.sortedWith(
            compareByDescending<InstalledApp> { it.isUserInstalled }
                .thenBy { it.appName.lowercase() }
        )
    }
    

}
