package com.datawatchdog.util

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppDataUsage(
    val packageName: String,
    val appName: String,
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
    private val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

    @RequiresApi(Build.VERSION_CODES.M)
    fun getAppDataUsage(): List<AppDataUsage> {
        if (!hasUsageStatsPermission()) {
            return emptyList()
        }

        val usageMap = mutableMapOf<String, AppDataUsage>()
        val now = System.currentTimeMillis()
        val startTime = now - (24 * 60 * 60 * 1000)

        try {
            // Query mobile data
            val mobileStats = networkStatsManager.querySummary(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startTime,
                now
            )

            processBuckets(mobileStats, usageMap, true)
            mobileStats.close()

            // Query WiFi data
            val wifiStats = networkStatsManager.querySummary(
                ConnectivityManager.TYPE_WIFI,
                null,
                startTime,
                now
            )

            processBuckets(wifiStats, usageMap, false)
            wifiStats.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return usageMap.values.filter { it.getTotal() > 0 }.sortedByDescending { it.getTotal() }
    }

    private fun processBuckets(
        networkStats: NetworkStats,
        usageMap: MutableMap<String, AppDataUsage>,
        isMobile: Boolean
    ) {
        val bucket = NetworkStats.Bucket()
        while (networkStats.hasNextBucket()) {
            networkStats.getNextBucket(bucket)
            val uid = bucket.uid
            val packageName = getPackageNameForUid(uid) ?: continue

            if (uid < 10000) continue

            val existing = usageMap[packageName] ?: AppDataUsage(
                packageName = packageName,
                appName = getAppName(packageName),
                mobileRx = 0,
                mobileTx = 0,
                wifiRx = 0,
                wifiTx = 0
            )

            usageMap[packageName] = if (isMobile) {
                existing.copy(
                    mobileRx = existing.mobileRx + bucket.rxBytes,
                    mobileTx = existing.mobileTx + bucket.txBytes
                )
            } else {
                existing.copy(
                    wifiRx = existing.wifiRx + bucket.rxBytes,
                    wifiTx = existing.wifiTx + bucket.txBytes
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    private fun getPackageNameForUid(uid: Int): String? {
        return try {
            packageManager.getPackagesForUid(uid)?.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun bytesToMB(bytes: Long): Double = bytes / (1024.0 * 1024.0)
}
