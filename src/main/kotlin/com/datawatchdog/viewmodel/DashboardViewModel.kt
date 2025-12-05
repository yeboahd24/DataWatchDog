package com.datawatchdog.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datawatchdog.db.AppDatabase
import com.datawatchdog.db.BundleEntity
import com.datawatchdog.util.AppDataUsage
import com.datawatchdog.util.DataUsageTracker
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

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val today = tracker.getTodayDate()
            val mobileUsage = db.dataUsageDao().getTotalMobileUsageForDate(today) ?: 0L
            val wifiUsage = db.dataUsageDao().getTotalWifiUsageForDate(today) ?: 0L

            _totalMobileUsage.value = tracker.bytesToMB(mobileUsage)
            _totalWifiUsage.value = tracker.bytesToMB(wifiUsage)

            val topAppsList = db.dataUsageDao().getTopAppsForDate(today)
                .map { entity ->
                    AppDataUsage(
                        packageName = entity.packageName,
                        appName = entity.appName,
                        mobileRx = entity.mobileRx,
                        mobileTx = entity.mobileTx,
                        wifiRx = entity.wifiRx,
                        wifiTx = entity.wifiTx
                    )
                }
            _topApps.value = topAppsList

            val bundle = db.bundleDao().getBundle()
            _bundleInfo.value = bundle
        }
    }

    fun refresh() {
        loadData()
    }
}
