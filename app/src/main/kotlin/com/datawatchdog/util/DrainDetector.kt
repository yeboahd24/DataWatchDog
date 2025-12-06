package com.datawatchdog.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.abs

data class DrainAlert(
    val appName: String,
    val packageName: String,
    val severity: AlertSeverity,
    val message: String,
    val dataUsed: Long,
    val percentage: Double,
    val recommendation: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AlertSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

data class DataDepletionPrediction(
    val willExceedBundle: Boolean,
    val projectedUsage: Long,
    val recommendedDailyUsage: Double,
    val daysUntilDepletion: Int,
    val confidence: Double
)

class DrainDetector(private val context: Context? = null) {
    private val usageHistory = mutableMapOf<String, MutableList<Long>>()
    private val maxHistorySize = 10
    private val prefs: SharedPreferences? = context?.getSharedPreferences("drain_detector", Context.MODE_PRIVATE)

    // Existing functionality
    fun recordUsage(packageName: String, bytesUsed: Long) {
        val history = usageHistory.getOrPut(packageName) { mutableListOf() }
        history.add(bytesUsed)
        if (history.size > maxHistorySize) {
            history.removeAt(0)
        }
    }

    fun isDraining(packageName: String): Boolean {
        val history = usageHistory[packageName] ?: return false
        if (history.size < 3) return false

        val recentUsage = history.takeLast(3)
        val avgUsage = recentUsage.average()
        val drainThresholdBytes = 2 * 1024 * 1024 // 2MB per minute

        return avgUsage > drainThresholdBytes
    }

    fun getDrainRate(packageName: String): Double {
        val history = usageHistory[packageName] ?: return 0.0
        if (history.isEmpty()) return 0.0

        val avgBytes = history.average()
        return avgBytes / (1024.0 * 1024.0) // Convert to MB
    }

    fun clearHistory(packageName: String) {
        usageHistory.remove(packageName)
    }

    fun clearAllHistory() {
        usageHistory.clear()
    }

    // Enhanced functionality
    fun detectDataDrains(
        usageData: List<AppDataUsage>,
        bundleLimit: Long? = null,
        daysRemaining: Int? = null
    ): Flow<DrainAlert> = flow {
        
        val totalUsage = usageData.sumOf { it.getTotal() }
        val thresholds = calculateThresholds(totalUsage, bundleLimit, daysRemaining)
        
        usageData.forEach { app ->
            val appTotal = app.getTotal()
            val percentage = if (totalUsage > 0) (appTotal.toDouble() / totalUsage.toDouble()) * 100.0 else 0.0
            
            // Record usage for trend analysis
            recordUsage(app.packageName, appTotal)
            
            // Check for high usage
            checkHighUsage(app, appTotal, percentage, thresholds)?.let { emit(it) }
            
            // Check for unusual patterns
            checkUsagePattern(app)?.let { emit(it) }
            
            // Check for background drains
            checkBackgroundDrain(app)?.let { emit(it) }
        }
        
        // Check overall bundle consumption
        bundleLimit?.let { limit ->
            checkBundleConsumptionRate(totalUsage, limit, daysRemaining)?.let { emit(it) }
        }
    }

    private data class UsageThresholds(
        val critical: Double,
        val high: Double,
        val medium: Double
    )
    
    private fun calculateThresholds(
        totalUsage: Long,
        bundleLimit: Long?,
        daysRemaining: Int?
    ): UsageThresholds {
        
        return if (bundleLimit != null && daysRemaining != null && daysRemaining > 0) {
            val recommendedDailyUsage = (bundleLimit - totalUsage).toDouble() / daysRemaining.toDouble()
            
            UsageThresholds(
                critical = recommendedDailyUsage * 0.4,
                high = recommendedDailyUsage * 0.25,
                medium = recommendedDailyUsage * 0.15
            )
        } else {
            UsageThresholds(
                critical = totalUsage * 0.25,
                high = totalUsage * 0.15,
                medium = totalUsage * 0.08
            )
        }
    }
    
    private fun checkHighUsage(
        app: AppDataUsage,
        appTotal: Long,
        percentage: Double,
        thresholds: UsageThresholds
    ): DrainAlert? {
        
        return when {
            appTotal > thresholds.critical -> DrainAlert(
                appName = app.appName,
                packageName = app.packageName,
                severity = AlertSeverity.CRITICAL,
                message = "🚨 Critical: ${formatBytes(appTotal)} (${percentage.toInt()}%)",
                dataUsed = appTotal,
                percentage = percentage,
                recommendation = "Restrict background data or switch to Wi-Fi"
            )
            
            appTotal > thresholds.high -> DrainAlert(
                appName = app.appName,
                packageName = app.packageName,
                severity = AlertSeverity.HIGH,
                message = "⚠️ High usage: ${formatBytes(appTotal)} (${percentage.toInt()}%)",
                dataUsed = appTotal,
                percentage = percentage,
                recommendation = "Monitor closely, prefer Wi-Fi when possible"
            )
            
            appTotal > thresholds.medium -> DrainAlert(
                appName = app.appName,
                packageName = app.packageName,
                severity = AlertSeverity.MEDIUM,
                message = "📊 Notable: ${formatBytes(appTotal)} (${percentage.toInt()}%)",
                dataUsed = appTotal,
                percentage = percentage,
                recommendation = "Keep an eye on this app's consumption"
            )
            
            else -> null
        }
    }
    
    private fun checkUsagePattern(app: AppDataUsage): DrainAlert? {
        val history = usageHistory[app.packageName]
        if (history == null || history.size < 3) return null
        
        val currentUsage = app.getTotal()
        val previousUsage = history[history.size - 2]
        
        if (previousUsage > 0) {
            val increase = currentUsage - previousUsage
            val increasePercentage = (increase.toDouble() / previousUsage) * 100
            
            // Detect sudden spikes
            if (increasePercentage > 150 && increase > 20 * 1024 * 1024) { // >150% increase, >20MB
                return DrainAlert(
                    appName = app.appName,
                    packageName = app.packageName,
                    severity = AlertSeverity.HIGH,
                    message = "📈 Usage spike: ${increasePercentage.toInt()}% increase",
                    dataUsed = currentUsage,
                    percentage = increasePercentage,
                    recommendation = "Check if app is downloading updates or syncing"
                )
            }
        }
        
        return null
    }
    
    private fun checkBackgroundDrain(app: AppDataUsage): DrainAlert? {
        val mobileUsage = app.mobileRx + app.mobileTx
        val totalUsage = app.getTotal()
        
        if (totalUsage > 50 * 1024 * 1024) { // >50MB total
            val mobileRatio = mobileUsage.toDouble() / totalUsage.toDouble()
            
            if (mobileRatio > 0.7) { // >70% mobile usage
                return DrainAlert(
                    appName = app.appName,
                    packageName = app.packageName,
                    severity = AlertSeverity.MEDIUM,
                    message = "📱 Prefers mobile: ${formatBytes(mobileUsage)} (${(mobileRatio * 100).toInt()}%)",
                    dataUsed = mobileUsage,
                    percentage = mobileRatio * 100,
                    recommendation = "Enable Wi-Fi preference in app settings"
                )
            }
        }
        
        return null
    }
    
    private fun checkBundleConsumptionRate(
        totalUsage: Long,
        bundleLimit: Long,
        daysRemaining: Int?
    ): DrainAlert? {
        
        if (daysRemaining == null || daysRemaining <= 0) return null
        
        val usagePercentage = (totalUsage.toDouble() / bundleLimit) * 100
        val daysElapsed = 30 - daysRemaining
        val expectedUsagePercentage = (daysElapsed.toDouble() / 30) * 100
        val consumptionRate = usagePercentage - expectedUsagePercentage
        
        return when {
            consumptionRate > 25 -> DrainAlert(
                appName = "Bundle Alert",
                packageName = "system.bundle",
                severity = AlertSeverity.CRITICAL,
                message = "🚨 Rapid depletion: ${usagePercentage.toInt()}% used, $daysRemaining days left",
                dataUsed = totalUsage,
                percentage = usagePercentage,
                recommendation = "Reduce usage significantly or buy additional bundle"
            )
            
            consumptionRate > 10 -> DrainAlert(
                appName = "Bundle Alert", 
                packageName = "system.bundle",
                severity = AlertSeverity.HIGH,
                message = "⚠️ Above average: ${usagePercentage.toInt()}% used, $daysRemaining days left",
                dataUsed = totalUsage,
                percentage = usagePercentage,
                recommendation = "Monitor usage and prioritize Wi-Fi"
            )
            
            else -> null
        }
    }
    
    fun predictDataDepletion(
        currentUsage: Long,
        bundleLimit: Long,
        daysRemaining: Int,
        recentDailyUsage: List<Long> = emptyList()
    ): DataDepletionPrediction? {
        
        if (daysRemaining <= 0 || bundleLimit <= 0) return null
        
        val remainingData = bundleLimit - currentUsage
        val dailyUsage = if (recentDailyUsage.isNotEmpty()) {
            recentDailyUsage.average()
        } else {
            currentUsage.toDouble() / (30 - daysRemaining)
        }
        
        val projectedTotalUsage = currentUsage + (dailyUsage * daysRemaining)
        val willExceed = projectedTotalUsage > bundleLimit
        val daysUntilDepletion = if (dailyUsage > 0) {
            (remainingData.toDouble() / dailyUsage).toInt()
        } else {
            daysRemaining
        }
        
        return DataDepletionPrediction(
            willExceedBundle = willExceed,
            projectedUsage = projectedTotalUsage.toLong(),
            recommendedDailyUsage = remainingData.toDouble() / daysRemaining,
            daysUntilDepletion = daysUntilDepletion,
            confidence = calculateConfidence(recentDailyUsage)
        )
    }
    
    private fun calculateConfidence(recentUsage: List<Long>): Double {
        if (recentUsage.size < 3) return 0.5
        
        val variance = recentUsage.map { abs(it - recentUsage.average()) }.average()
        val mean = recentUsage.average()
        val coefficientOfVariation = if (mean > 0) variance / mean else 1.0
        
        return kotlin.math.max(0.1, 1.0 - coefficientOfVariation)
    }
    
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
