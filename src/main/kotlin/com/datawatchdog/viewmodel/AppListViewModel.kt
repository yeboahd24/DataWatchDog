package com.datawatchdog.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datawatchdog.db.AppDatabase
import com.datawatchdog.util.AppDataUsage
import com.datawatchdog.util.DataUsageTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppListViewModel(context: Context) : ViewModel() {
    private val db = AppDatabase.getDatabase(context)
    private val tracker = DataUsageTracker(context)

    private val _allApps = MutableStateFlow<List<AppDataUsage>>(emptyList())
    val allApps: StateFlow<List<AppDataUsage>> = _allApps

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val today = tracker.getTodayDate()
            val appsList = db.dataUsageDao().getUsageForDate(today)
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
                .sortedByDescending { it.getTotal() }
            _allApps.value = appsList
        }
    }

    fun refresh() {
        loadData()
    }
}
