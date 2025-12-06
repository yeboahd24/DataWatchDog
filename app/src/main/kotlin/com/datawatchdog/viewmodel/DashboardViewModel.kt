package com.datawatchdog.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datawatchdog.db.AppDatabase
import com.datawatchdog.db.BundleEntity
import com.datawatchdog.util.AppDataUsage
import com.datawatchdog.util.DataUsageTracker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(context: Context) : ViewModel() {
    private val db = AppDatabase.getDatabase(context)
    private val tracker = DataUsageTracker(context)

    private val _totalMobileUsage = MutableStateFlow(0.0)
    val totalMobileUsage: StateFlow<Double> = _totalMobileUsage

    private val _totalWifiUsage = MutableStateFlow(0.0)
    val totalWifiUsage: StateFlow<Double> = _totalWifiUsage

    private val _topApps = MutableStateFlow<List<AppDataUsage>>(emptyList())
    val topApps: StateFlow<List<AppDataUsage>> = _topApps

    private val _bundleInfo = MutableStateFlow<BundleEntity?>(null)
    val bundleInfo: StateFlow<BundleEntity?> = _bundleInfo

    private val _drainAlerts = MutableStateFlow<List<com.datawatchdog.db.DrainAlertEntity>>(emptyList())
    val drainAlerts: StateFlow<List<com.datawatchdog.db.DrainAlertEntity>> = _drainAlerts

    init {
        loadData()
        startAutoRefresh()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                // Get live data from NetworkStatsManager
                val appUsageList = tracker.getAppDataUsage()
                
                val mobileUsage = appUsageList.sumOf { it.mobileRx + it.mobileTx }
                val wifiUsage = appUsageList.sumOf { it.wifiRx + it.wifiTx }

                _totalMobileUsage.value = tracker.bytesToMB(mobileUsage)
                _totalWifiUsage.value = tracker.bytesToMB(wifiUsage)
                _topApps.value = appUsageList.sortedByDescending { it.getTotal() }.take(5)

                // Get bundle info
                val bundle = db.bundleDao().getBundle()
                _bundleInfo.value = bundle

                // Get recent drain alerts (last hour)
                val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
                val alerts = db.drainAlertDao().getAlertsAfter(oneHourAgo)
                _drainAlerts.value = alerts
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(10000) // Refresh every 10 seconds
                loadData()
            }
        }
    }

    fun refresh() {
        loadData()
    }
}
