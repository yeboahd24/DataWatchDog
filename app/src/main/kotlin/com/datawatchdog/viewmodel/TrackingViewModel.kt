package com.datawatchdog.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datawatchdog.db.AppDatabase
import com.datawatchdog.db.AppTrackingEntity
import com.datawatchdog.util.DataUsageTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TrackingViewModel(private val context: Context) : ViewModel() {
    private val db = AppDatabase.getDatabase(context)
    private val tracker = DataUsageTracker(context)

    private val _activeTracking = MutableStateFlow<AppTrackingEntity?>(null)
    val activeTracking: StateFlow<AppTrackingEntity?> = _activeTracking

    private val _completedTrackings = MutableStateFlow<List<AppTrackingEntity>>(emptyList())
    val completedTrackings: StateFlow<List<AppTrackingEntity>> = _completedTrackings

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _activeTracking.value = db.appTrackingDao().getActiveTracking()
            _completedTrackings.value = db.appTrackingDao().getCompletedTrackings()
        }
    }

    fun startTracking(packageName: String, appName: String) {
        viewModelScope.launch {
            val usageList = tracker.getAppDataUsage()
            val appUsage = usageList.find { it.packageName == packageName }

            if (appUsage != null) {
                val tracking = AppTrackingEntity(
                    packageName = packageName,
                    appName = appName,
                    startTime = System.currentTimeMillis(),
                    startMobileRx = appUsage.mobileRx,
                    startMobileTx = appUsage.mobileTx,
                    startWifiRx = appUsage.wifiRx,
                    startWifiTx = appUsage.wifiTx,
                    isActive = true
                )
                db.appTrackingDao().insertTracking(tracking)
                loadData()
            }
        }
    }

    fun stopTracking() {
        viewModelScope.launch {
            val active = _activeTracking.value ?: return@launch
            val usageList = tracker.getAppDataUsage()
            val appUsage = usageList.find { it.packageName == active.packageName }

            if (appUsage != null) {
                val updated = active.copy(
                    endTime = System.currentTimeMillis(),
                    endMobileRx = appUsage.mobileRx,
                    endMobileTx = appUsage.mobileTx,
                    endWifiRx = appUsage.wifiRx,
                    endWifiTx = appUsage.wifiTx,
                    isActive = false
                )
                db.appTrackingDao().updateTracking(updated)
                loadData()
            }
        }
    }

    fun deleteTracking(id: Int) {
        viewModelScope.launch {
            db.appTrackingDao().deleteTracking(id)
            loadData()
        }
    }

    fun refresh() {
        loadData()
    }
}
