package com.datawatchdog.util

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkStats
import android.net.NetworkStatsManager
import android.os.Build
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
    private val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
    private val packageManager = context.packageManager
    private val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

    fun getAppDataUsage(): List<AppDataUsage> {
        val usageMap = mutableMapOf<String, AppDataUsage>()
        val now = System.currentTimeMillis()
        val startTime = now - (24 * 60 * 60 * 1000) // Last 24 hours

        try {
            // Mobile data
            val mobileStats = networkStatsManager.queryDetailsForUid(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startTime,
                now,
                NetworkStatsManager.UID_ALL
            )

            while (mobileStats.hasNextBucket()) {
                val bucket = NetworkStats.Bucket()
                mobileStats.getNextBucket(bucket)
                val uid = bucket.uid
                val packageName = getPackageNameForUid(uid) ?: continue

                val existing = usageMap[packageName] ?: AppDataUsage(
                    packageName = packageName,
                    appName = getAppName(packageName),
                    mobileRx = 0,
                    mobileTx = 0,
                    wifiRx = 0,
                    wifiTx = 0
                )

                usageMap[packageName] = existing.copy(
                    mobileRx = existing.mobileRx + bucket.rxBytes,
                    mobileTx = existing.mobileTx + bucket.txBytes
                )
            }
            mobileStats.close()

            // WiFi data
            val wifiStats = networkStatsManager.queryDetailsForUid(
                ConnectivityManager.TYPE_WIFI,
                null,
                startTime,
                now,
                NetworkStatsManager.UID_ALL
            )

            while (wifiStats.hasNextBucket()) {
                val bucket = NetworkStats.Bucket()
                wifiStats.getNextBucket(bucket)
                val uid = bucket.uid
                val packageName = getPackageNameForUid(uid) ?: continue

                val existing = usageMap[packageName] ?: AppDataUsage(
                    packageName = packageName,
                    appName = getAppName(packageName),
                    mobileRx = 0,
                    mobileTx = 0,
                    wifiRx = 0,
                    wifiTx = 0
                )

                usageMap[packageName] = existing.copy(
                    wifiRx = existing.wifiRx + bucket.rxBytes,
                    wifiTx = existing.wifiTx + bucket.txBytes
                )
            }
            wifiStats.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return usageMap.values.filter { it.getTotal() > 0 }
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
