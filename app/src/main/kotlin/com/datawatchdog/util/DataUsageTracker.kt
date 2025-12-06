package com.datawatchdog.util

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.ApplicationInfo
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
    private val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

    @RequiresApi(Build.VERSION_CODES.M)
    fun getAppDataUsage(): List<AppDataUsage> {
        val usageMap = mutableMapOf<String, AppDataUsage>()
        val now = System.currentTimeMillis()
        val startTime = now - (24 * 60 * 60 * 1000) // Last 24 hours

        try {
            // Use reflection to access NetworkStatsManager safely
            val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE)
                ?: return emptyList()

            // Get the classes using reflection to avoid compile-time dependencies
            val networkStatsManagerClass = networkStatsManager::class.java
            val networkStatsClass = Class.forName("android.net.NetworkStats")
            val bucketClass = Class.forName("android.net.NetworkStats\$Bucket")

            // Get the query method
            val queryMethod = networkStatsManagerClass.getMethod(
                "queryDetailsForUid",
                Int::class.java,
                String::class.java,
                Long::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )

            // Constants for connection types (avoiding direct import)
            val typeMobile = 0 // ConnectivityManager.TYPE_MOBILE
            val typeWifi = 1   // ConnectivityManager.TYPE_WIFI
            val uidAll = -1    // NetworkStatsManager.UID_ALL

            // Process mobile data
            processNetworkStats(
                queryMethod.invoke(networkStatsManager, typeMobile, null, startTime, now, uidAll),
                networkStatsClass,
                bucketClass,
                usageMap,
                true // isMobile
            )

            // Process WiFi data
            processNetworkStats(
                queryMethod.invoke(networkStatsManager, typeWifi, null, startTime, now, uidAll),
                networkStatsClass,
                bucketClass,
                usageMap,
                false // isWiFi
            )

        } catch (e: Exception) {
            // Gracefully handle any reflection or permission errors
            println("DataUsageTracker: Error accessing NetworkStats: ${e.message}")
        }

        return usageMap.values
            .filter { it.getTotal() > 0 }
            .sortedByDescending { it.getTotal() }
    }

    private fun processNetworkStats(
        networkStats: Any?,
        networkStatsClass: Class<*>,
        bucketClass: Class<*>,
        usageMap: MutableMap<String, AppDataUsage>,
        isMobile: Boolean
    ) {
        if (networkStats == null) return

        try {
            val hasNextBucketMethod = networkStatsClass.getMethod("hasNextBucket")
            val getNextBucketMethod = networkStatsClass.getMethod("getNextBucket", bucketClass)
            val closeMethod = networkStatsClass.getMethod("close")
            val bucketConstructor = bucketClass.getConstructor()

            // Get bucket fields
            val uidField = bucketClass.getField("uid")
            val rxBytesField = bucketClass.getField("rxBytes")
            val txBytesField = bucketClass.getField("txBytes")

            while (hasNextBucketMethod.invoke(networkStats) as Boolean) {
                val bucket = bucketConstructor.newInstance()
                getNextBucketMethod.invoke(networkStats, bucket)

                val uid = uidField.getInt(bucket)
                val rxBytes = rxBytesField.getLong(bucket)
                val txBytes = txBytesField.getLong(bucket)

                val packageName = getPackageNameForUid(uid) ?: continue

                // Skip system UIDs and invalid packages
                if (uid < 10000 || packageName.isEmpty()) continue

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
                        mobileRx = existing.mobileRx + rxBytes,
                        mobileTx = existing.mobileTx + txBytes
                    )
                } else {
                    existing.copy(
                        wifiRx = existing.wifiRx + rxBytes,
                        wifiTx = existing.wifiTx + txBytes
                    )
                }
            }

            // Close the NetworkStats object
            closeMethod.invoke(networkStats)

        } catch (e: Exception) {
            println("DataUsageTracker: Error processing network stats: ${e.message}")
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
