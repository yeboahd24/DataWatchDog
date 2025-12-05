package com.datawatchdog.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datawatchdog.db.AppDatabase
import com.datawatchdog.db.BundleEntity
import com.datawatchdog.util.BundlePredictor
import com.datawatchdog.util.DataUsageTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PredictionData(
    val exhaustionTime: Long,
    val avgUsagePerMinute: Double
)

class BundleViewModel(context: Context) : ViewModel() {
    private val db = AppDatabase.getDatabase(context)
    private val tracker = DataUsageTracker(context)
    private val predictor = BundlePredictor()

    private val _bundleInfo = MutableStateFlow<BundleEntity?>(null)
    val bundleInfo: StateFlow<BundleEntity?> = _bundleInfo

    private val _exhaustionPrediction = MutableStateFlow<PredictionData?>(null)
    val exhaustionPrediction: StateFlow<PredictionData?> = _exhaustionPrediction

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val bundle = db.bundleDao().getBundle()
            _bundleInfo.value = bundle

            if (bundle != null) {
                val today = tracker.getTodayDate()
                val usageList = db.dataUsageDao().getUsageForDate(today)
                val totalUsage = usageList.sumOf { it.mobileRx + it.mobileTx }

                predictor.recordUsage(totalUsage)
                val exhaustionTime = predictor.predictBundleExhaustionTime(
                    bundle.totalMB - bundle.usedMB
                )

                if (exhaustionTime > 0) {
                    _exhaustionPrediction.value = PredictionData(
                        exhaustionTime = exhaustionTime,
                        avgUsagePerMinute = predictor.getAverageUsagePerMinute()
                    )
                }
            }
        }
    }

    fun refresh() {
        loadData()
    }
}
