package com.datawatchdog.util

import java.util.concurrent.TimeUnit

class BundlePredictor {
    private val usageHistory = mutableListOf<Long>()
    private val maxHistorySize = 10

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
}
