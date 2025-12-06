package com.datawatchdog.util

import android.content.Context
import android.content.pm.PackageManager
import android.net.TrafficStats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppDataUsage(
    val packageName: String,
    val appName: String,
    val uid: Int,
    val mobileRx: Long,
    val mobileTx: Long,
    val wifiRx: Long,
    val wifiTx: Long
) {
    fun getTotalMobile() = mobileRx + mobileTx
    fun getTotalWifi() = wifiRx + wifiTx
    fun getTotal() = getTotalMobile() + getTotalWifi()
}

class DataUsageTracker(private val context: Context) {
    private val packageManager = context.packageManager

    /**
     * Get data usage for all apps using TrafficStats API
     * No permissions required, works since device boot
     */
    fun getAppDataUsage(): List<AppDataUsage> {
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val results = mutableListOf<AppDataUsage>()

        apps.forEach { appInfo ->
            val uid = appInfo.uid

            // Get total data usage since boot (TrafficStats doesn't separate mobile/WiFi)
            val totalRx = TrafficStats.getUidRxBytes(uid)
            val totalTx = TrafficStats.getUidTxBytes(uid)

            // TrafficStats returns -1 if not supported or no data
            if (totalRx < 0 || totalTx < 0) return@forEach

            // Skip if no usage
            if (totalRx + totalTx == 0L) return@forEach

            // For now, we'll treat all traffic as "mobile" since TrafficStats doesn't separate
            // In a future update, we can add logic to estimate mobile vs WiFi
            results.add(
                AppDataUsage(
                    packageName = appInfo.packageName,
                    appName = appInfo.loadLabel(packageManager).toString(),
                    uid = uid,
                    mobileRx = totalRx, // Treating all as mobile for simplicity
                    mobileTx = totalTx,
                    wifiRx = 0L, // Could be enhanced later
                    wifiTx = 0L
                )
            )
        }

        return results.sortedByDescending { it.getTotal() }
    }

    /**
     * Get data usage for a specific app by package name
     */
    fun getAppDataUsageByPackage(packageName: String): AppDataUsage? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val uid = appInfo.uid

            val totalRx = TrafficStats.getUidRxBytes(uid)
            val totalTx = TrafficStats.getUidTxBytes(uid)

            if (totalRx < 0 || totalTx < 0) return null

            AppDataUsage(
                packageName = packageName,
                appName = appInfo.loadLabel(packageManager).toString(),
                uid = uid,
                mobileRx = totalRx,
                mobileTx = totalTx,
                wifiRx = 0L,
                wifiTx = 0L
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun bytesToMB(bytes: Long): Double = bytes / (1024.0 * 1024.0)
}
