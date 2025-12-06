package com.datawatchdog.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datawatchdog.db.AppDatabase
import com.datawatchdog.util.AppDataUsage
import com.datawatchdog.util.DataUsageTracker
import kotlinx.coroutines.delay
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
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(10000)
                loadData()
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            val appsList = tracker.getAppDataUsage()
            _allApps.value = appsList
        }
    }

    fun refresh() {
        loadData()
    }
}
