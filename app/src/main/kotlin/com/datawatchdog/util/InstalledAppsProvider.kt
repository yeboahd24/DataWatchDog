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
        val packages = packageManager.getInstalledApplications(0) // Use 0 instead of GET_META_DATA
        
        val allApps = packages.mapNotNull { appInfo ->
            try {
                val packageName = appInfo.packageName
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                
                // Correct logic based on your research:
                // User apps are those that are NOT system apps AND NOT updated system apps
                val isUserApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                               (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
                
                // However, we also want to include updated system apps that users interact with
                // like YouTube, Chrome, etc. (apps that came with the system but got updated)
                val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                
                // For tracking purposes, we want both user-installed apps AND updated system apps
                // because both represent apps that users actively use
                val isUserInstalled = isUserApp || isUpdatedSystemApp
                
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
    

}
