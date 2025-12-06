package com.datawatchdog.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datawatchdog.db.AppDatabase
import com.datawatchdog.db.BundleEntity
import com.datawatchdog.util.AppDataUsage
import com.datawatchdog.util.BundlePrediction
import com.datawatchdog.util.BundlePredictor
import com.datawatchdog.util.DataUsageTracker
import com.datawatchdog.util.DrainAlert
import com.datawatchdog.util.DrainDetector
import com.datawatchdog.util.SmsParser
import com.datawatchdog.util.UsageAnalytics
import com.datawatchdog.util.UsageTrend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.max

data class DashboardUiState(
    val currentUsage: Long = 0L,
    val bundleLimit: Long = 0L,
    val daysRemaining: Int = 0,
    val todayUsage: Long = 0L,
    val avgDailyUsage: Double = 0.0,
    val topApps: List<AppDataUsage> = emptyList(),
    val dataAlerts: List<DrainAlert> = emptyList(),
    val bundlePrediction: BundlePrediction? = null,
    val usageTrend: UsageTrend = UsageTrend.STABLE,
    val usageAnalytics: UsageAnalytics? = null,
    val isLoading: Boolean = false
)

class DashboardViewModel(private val context: Context) : ViewModel() {
    private val db = AppDatabase.getDatabase(context)
    private val tracker = DataUsageTracker(context)
    private val drainDetector = DrainDetector(context)
    private val bundlePredictor = BundlePredictor(context)
    private val smsParser = SmsParser(context)

    // Original state flows for backward compatibility
    private val _totalMobileUsage = MutableStateFlow(0.0)
    val totalMobileUsage: StateFlow<Double> = _totalMobileUsage

    private val _totalWifiUsage = MutableStateFlow(0.0)
    val totalWifiUsage: StateFlow<Double> = _totalWifiUsage

    private val _topApps = MutableStateFlow<List<AppDataUsage>>(emptyList())
    val topApps: StateFlow<List<AppDataUsage>> = _topApps

    private val _bundleInfo = MutableStateFlow<BundleEntity?>(null)
    val bundleInfo: StateFlow<BundleEntity?> = _bundleInfo

    // New enhanced UI state
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        loadData()
        startPeriodicUpdates()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Load current data usage
                val appUsageList = tracker.getAppDataUsage()
                val totalUsage = appUsageList.sumOf { it.getTotal() }
                val mobileUsage = appUsageList.sumOf { it.mobileRx + it.mobileTx }
                val wifiUsage = appUsageList.sumOf { it.wifiRx + it.wifiTx }
                
                // Update original flows for compatibility
                _totalMobileUsage.value = tracker.bytesToMB(mobileUsage)
                _totalWifiUsage.value = tracker.bytesToMB(wifiUsage)
                _topApps.value = appUsageList.take(5)
                
                // Load bundle information
                val bundleEntity = db.bundleDao().getBundle()
                _bundleInfo.value = bundleEntity
                
                // Parse SMS for latest bundle info if available
                val smsBundle = smsParser.parseBundleFromSms()
                val bundleLimit = smsBundle?.totalMB?.let { it * 1024 * 1024 } ?: (bundleEntity?.totalMB?.times(1024 * 1024) ?: 5L * 1024 * 1024 * 1024) // Default 5GB
                val bundleUsed = smsBundle?.usedMB?.let { it * 1024 * 1024 } ?: totalUsage
                val daysRemaining = calculateDaysRemaining(smsBundle?.expiryDate ?: bundleEntity?.expiryDate)
                
                // Generate data alerts
                val alerts = mutableListOf<DrainAlert>()
                drainDetector.detectDataDrains(appUsageList, bundleLimit, daysRemaining)
                    .collectLatest { alert ->
                        alerts.add(alert)
                    }
                
                // Generate bundle prediction
                val dailyUsageHistory = getDailyUsageHistory(7) // Last 7 days
                val bundlePrediction = bundlePredictor.predictUsage(
                    totalBundleSize = bundleLimit,
                    usedSoFar = bundleUsed,
                    daysElapsed = 30 - daysRemaining,
                    totalDays = 30,
                    dailyUsageHistory = dailyUsageHistory
                )
                
                // Analyze usage patterns
                val dailyUsageWithDates = getDailyUsageWithDates(7)
                val usageAnalytics = bundlePredictor.analyzeUsagePatterns(dailyUsageWithDates)
                val usageTrend = detectTrend(dailyUsageHistory)
                
                // Calculate today's usage and daily average
                val todayUsage = calculateTodayUsage(appUsageList)
                val avgDailyUsage = if (dailyUsageHistory.isNotEmpty()) {
                    dailyUsageHistory.average()
                } else {
                    totalUsage.toDouble() / max(1, 30 - daysRemaining)
                }
                
                _uiState.value = DashboardUiState(
                    currentUsage = bundleUsed,
                    bundleLimit = bundleLimit,
                    daysRemaining = daysRemaining,
                    todayUsage = todayUsage,
                    avgDailyUsage = avgDailyUsage,
                    topApps = appUsageList.take(5),
                    dataAlerts = alerts.take(5), // Limit alerts for UI
                    bundlePrediction = bundlePrediction,
                    usageTrend = usageTrend,
                    usageAnalytics = usageAnalytics,
                    isLoading = false
                )
                
            } catch (e: Exception) {
                println("DashboardViewModel: Error loading data: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun startPeriodicUpdates() {
        viewModelScope.launch {
            // Update data every 5 minutes
            kotlinx.coroutines.delay(5 * 60 * 1000) // 5 minutes
            loadData()
        }
    }

    private fun calculateDaysRemaining(expiryDate: Long?): Int {
        if (expiryDate == null) return 15 // Default fallback
        val currentTime = System.currentTimeMillis()
        val timeDiff = expiryDate - currentTime
        return max(0, (timeDiff / (24 * 60 * 60 * 1000)).toInt())
    }

    private fun getDailyUsageHistory(days: Int): List<Long> {
        // In a real implementation, this would query the database for historical daily usage
        // For now, return simulated data
        return listOf(
            500 * 1024 * 1024L, // 500MB
            750 * 1024 * 1024L, // 750MB  
            600 * 1024 * 1024L, // 600MB
            800 * 1024 * 1024L, // 800MB
            550 * 1024 * 1024L, // 550MB
            700 * 1024 * 1024L, // 700MB
            650 * 1024 * 1024L  // 650MB
        ).take(days)
    }

    private fun getDailyUsageWithDates(days: Int): List<Pair<String, Long>> {
        val usage = getDailyUsageHistory(days)
        val currentTime = System.currentTimeMillis()
        
        return usage.mapIndexed { index, usageBytes ->
            val date = "Day-${days - index}"
            date to usageBytes
        }
    }

    private fun detectTrend(dailyUsage: List<Long>): UsageTrend {
        if (dailyUsage.size < 3) return UsageTrend.STABLE
        
        val recent = dailyUsage.takeLast(2).average()
        val earlier = dailyUsage.take(2).average()
        val change = (recent - earlier) / earlier
        
        return when {
            change > 0.2 -> UsageTrend.INCREASING
            change < -0.2 -> UsageTrend.DECREASING
            dailyUsage.last() > recent * 1.5 -> UsageTrend.SPIKE
            else -> UsageTrend.STABLE
        }
    }

    private fun calculateTodayUsage(appUsageList: List<AppDataUsage>): Long {
        // This would typically query for today's specific usage
        // For now, return the current total as a proxy
        return appUsageList.sumOf { it.getTotal() }
    }

    fun refresh() {
        loadData()
    }

    fun dismissAlert(alert: DrainAlert) {
        val currentAlerts = _uiState.value.dataAlerts.toMutableList()
        currentAlerts.remove(alert)
        _uiState.value = _uiState.value.copy(dataAlerts = currentAlerts)
    }

    fun recordDataUsage(packageName: String, bytesUsed: Long) {
        drainDetector.recordUsage(packageName, bytesUsed)
        bundlePredictor.recordUsage(bytesUsed)
    }
}
