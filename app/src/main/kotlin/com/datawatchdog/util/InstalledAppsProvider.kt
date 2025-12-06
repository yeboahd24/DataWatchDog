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
                val isUserApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 ||
                                packageManager.getLaunchIntentForPackage(appInfo.packageName) != null
                
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
