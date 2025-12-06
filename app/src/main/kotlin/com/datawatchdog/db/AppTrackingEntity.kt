package com.datawatchdog.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_tracking")
data class AppTrackingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val packageName: String,
    val appName: String,
    val startTime: Long,
    val endTime: Long? = null,
    val startMobileRx: Long,
    val startMobileTx: Long,
    val startWifiRx: Long,
    val startWifiTx: Long,
    val endMobileRx: Long? = null,
    val endMobileTx: Long? = null,
    val endWifiRx: Long? = null,
    val endWifiTx: Long? = null,
    val isActive: Boolean = true
) {
    fun getTotalMobileUsed(): Long {
        if (endMobileRx == null || endMobileTx == null) return 0
        return (endMobileRx - startMobileRx) + (endMobileTx - startMobileTx)
    }
    
    fun getTotalWifiUsed(): Long {
        if (endWifiRx == null || endWifiTx == null) return 0
        return (endWifiRx - startWifiRx) + (endWifiTx - startWifiTx)
    }
    
    fun getTotalUsed(): Long = getTotalMobileUsed() + getTotalWifiUsed()
}
