package com.datawatchdog.util

import kotlin.math.abs

class DrainDetector {
    private val usageHistory = mutableMapOf<String, MutableList<Long>>()
    private val maxHistorySize = 10

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
}
