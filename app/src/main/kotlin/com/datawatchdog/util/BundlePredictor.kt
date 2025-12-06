package com.datawatchdog.util

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

data class BundlePrediction(
    val willFinishEarly: Boolean,
    val daysToFinish: Int,
    val recommendedDailyUsage: Double,
    val confidence: Double,
    val projectedOverage: Long = 0,
    val savings: Long = 0,
    val trend: UsageTrend = UsageTrend.STABLE
)

data class UsageAnalytics(
    val averageDailyUsage: Double,
    val peakUsageDays: List<String>,
    val lightUsageDays: List<String>,
    val weekdayVsWeekendRatio: Double,
    val mostActiveHours: List<Int>,
    val dataEfficiencyScore: Double
)

enum class UsageTrend {
    INCREASING, DECREASING, STABLE, SPIKE
}

class BundlePredictor(private val context: Context? = null) {
    private val usageHistory = mutableListOf<Long>()
    private val maxHistorySize = 10
    private val prefs: SharedPreferences? = context?.getSharedPreferences("bundle_predictor", Context.MODE_PRIVATE)

    // Existing functionality
    fun recordUsage(bytesUsed: Long) {
        usageHistory.add(bytesUsed)
        if (usageHistory.size > maxHistorySize) {
            usageHistory.removeAt(0)
        }
    }

    fun predictBundleExhaustionTime(remainingMB: Long): Long {
        if (usageHistory.size < 2) return -1

        val avgBytesPerMinute = usageHistory.average()
        if (avgBytesPerMinute <= 0) return -1

        val remainingBytes = remainingMB * 1024 * 1024
        val minutesRemaining = remainingBytes / avgBytesPerMinute
        val millisRemaining = (minutesRemaining * 60 * 1000).toLong()

        return System.currentTimeMillis() + millisRemaining
    }

    fun getAverageUsagePerMinute(): Double {
        return if (usageHistory.isEmpty()) 0.0 else usageHistory.average() / (1024.0 * 1024.0)
    }

    fun clear() {
        usageHistory.clear()
    }

    // Enhanced functionality
    fun predictUsage(
        totalBundleSize: Long,
        usedSoFar: Long,
        daysElapsed: Int,
        totalDays: Int,
        dailyUsageHistory: List<Long> = emptyList()
    ): BundlePrediction {
        
        val remainingData = totalBundleSize - usedSoFar
        val remainingDays = totalDays - daysElapsed
        
        // Calculate usage rate with trend analysis
        val dailyUsageRate = if (dailyUsageHistory.isNotEmpty()) {
            calculateTrendAdjustedUsage(dailyUsageHistory)
        } else {
            usedSoFar.toDouble() / maxOf(daysElapsed, 1)
        }
        
        val projectedTotalUsage = usedSoFar + (dailyUsageRate * remainingDays)
        val willFinishEarly = projectedTotalUsage > totalBundleSize
        
        val daysToFinish = if (dailyUsageRate > 0) {
            (totalBundleSize.toDouble() / dailyUsageRate).toInt()
        } else {
            totalDays
        }
        
        val recommendedDailyUsage = if (remainingDays > 0) {
            remainingData.toDouble() / remainingDays
        } else {
            0.0
        }
        
        val confidence = calculateConfidence(daysElapsed, dailyUsageHistory)
        val trend = detectUsageTrend(dailyUsageHistory)
        
        val projectedOverage = if (willFinishEarly) {
            (projectedTotalUsage - totalBundleSize).toLong()
        } else {
            0L
        }
        
        val savings = if (!willFinishEarly) {
            totalBundleSize - projectedTotalUsage.toLong()
        } else {
            0L
        }
        
        return BundlePrediction(
            willFinishEarly = willFinishEarly,
            daysToFinish = daysToFinish,
            recommendedDailyUsage = recommendedDailyUsage,
            confidence = confidence,
            projectedOverage = projectedOverage,
            savings = savings,
            trend = trend
        )
    }

    fun analyzeUsagePatterns(dailyUsageHistory: List<Pair<String, Long>>): UsageAnalytics {
        if (dailyUsageHistory.isEmpty()) {
            return UsageAnalytics(0.0, emptyList(), emptyList(), 1.0, emptyList(), 0.5)
        }
        
        val usageValues = dailyUsageHistory.map { it.second }
        val averageDailyUsage = usageValues.average()
        
        // Find peak and light usage days
        val sortedByUsage = dailyUsageHistory.sortedByDescending { it.second }
        val peakDays = sortedByUsage.take(3).map { it.first }
        val lightDays = sortedByUsage.takeLast(3).map { it.first }
        
        // Calculate weekday vs weekend ratio (simplified)
        val weekdayUsage = usageValues.take(5).averageOrNull() ?: 0.0
        val weekendUsage = usageValues.takeLast(2).averageOrNull() ?: 0.0
        val weekdayVsWeekendRatio = if (weekendUsage > 0) weekdayUsage / weekendUsage else 1.0
        
        // Simulate most active hours
        val mostActiveHours = listOf(9, 12, 18, 21)
        
        // Calculate data efficiency score
        val variance = usageValues.map { (it - averageDailyUsage).pow(2) }.average()
        val standardDeviation = sqrt(variance)
        val coefficientOfVariation = if (averageDailyUsage > 0) standardDeviation / averageDailyUsage else 1.0
        val dataEfficiencyScore = maxOf(0.0, 1.0 - coefficientOfVariation)
        
        return UsageAnalytics(
            averageDailyUsage = averageDailyUsage,
            peakUsageDays = peakDays,
            lightUsageDays = lightDays,
            weekdayVsWeekendRatio = weekdayVsWeekendRatio,
            mostActiveHours = mostActiveHours,
            dataEfficiencyScore = dataEfficiencyScore
        )
    }

    private fun calculateTrendAdjustedUsage(dailyUsageHistory: List<Long>): Double {
        if (dailyUsageHistory.size < 3) {
            return dailyUsageHistory.average()
        }
        
        // Give more weight to recent usage
        val weights = dailyUsageHistory.indices.map { index ->
            1.0 + (index.toDouble() / dailyUsageHistory.size) * 0.5
        }
        
        val weightedSum = dailyUsageHistory.zip(weights) { usage, weight ->
            usage * weight
        }.sum()
        
        val totalWeight = weights.sum()
        
        return weightedSum / totalWeight
    }
    
    private fun detectUsageTrend(dailyUsageHistory: List<Long>): UsageTrend {
        if (dailyUsageHistory.size < 5) return UsageTrend.STABLE
        
        val recent = dailyUsageHistory.takeLast(3).average()
        val earlier = dailyUsageHistory.drop(dailyUsageHistory.size - 6).take(3).average()
        val change = (recent - earlier) / earlier
        
        // Check for sudden spikes
        val lastValue = dailyUsageHistory.last()
        val previousAverage = dailyUsageHistory.dropLast(1).takeLast(3).averageOrNull() ?: 0.0
        val spikeRatio = if (previousAverage > 0) lastValue / previousAverage else 1.0
        
        return when {
            spikeRatio > 2.0 -> UsageTrend.SPIKE
            change > 0.2 -> UsageTrend.INCREASING
            change < -0.2 -> UsageTrend.DECREASING
            else -> UsageTrend.STABLE
        }
    }
    
    private fun calculateConfidence(daysElapsed: Int, dailyUsageHistory: List<Long>): Double {
        var confidence = minOf(0.9, daysElapsed / 14.0) // Base confidence from time elapsed
        
        // Adjust based on data consistency
        if (dailyUsageHistory.size >= 3) {
            val variance = dailyUsageHistory.map { 
                abs(it - dailyUsageHistory.average()) 
            }.average()
            val mean = dailyUsageHistory.average()
            val coefficientOfVariation = if (mean > 0) variance / mean else 1.0
            
            // Lower variance = higher confidence
            val consistencyScore = 1.0 - minOf(1.0, coefficientOfVariation)
            confidence = (confidence + consistencyScore) / 2.0
        }
        
        return maxOf(0.1, confidence)
    }

    private fun List<Long>.averageOrNull(): Double? {
        return if (isEmpty()) null else average()
    }
}
