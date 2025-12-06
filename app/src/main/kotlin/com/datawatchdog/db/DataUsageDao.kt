package com.datawatchdog.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface DataUsageDao {
    @Insert
    suspend fun insertUsage(usage: DataUsageEntity)

    @Query("SELECT * FROM data_usage WHERE date = :date ORDER BY (mobileRx + mobileTx + wifiRx + wifiTx) DESC LIMIT 5")
    suspend fun getTopAppsForDate(date: String): List<DataUsageEntity>

    @Query("SELECT * FROM data_usage WHERE date = :date")
    suspend fun getUsageForDate(date: String): List<DataUsageEntity>

    @Query("SELECT SUM(mobileRx + mobileTx) as total FROM data_usage WHERE date = :date")
    suspend fun getTotalMobileUsageForDate(date: String): Long?

    @Query("SELECT SUM(wifiRx + wifiTx) as total FROM data_usage WHERE date = :date")
    suspend fun getTotalWifiUsageForDate(date: String): Long?

    @Query("DELETE FROM data_usage WHERE timestamp < :cutoffTime")
    suspend fun deleteOldData(cutoffTime: Long)

    @Query("SELECT packageName, appName, SUM(mobileRx) as mobileRx, SUM(mobileTx) as mobileTx, SUM(wifiRx) as wifiRx, SUM(wifiTx) as wifiTx FROM data_usage WHERE timestamp >= :startTime AND timestamp <= :endTime GROUP BY packageName ORDER BY (SUM(mobileRx) + SUM(mobileTx) + SUM(wifiRx) + SUM(wifiTx)) DESC")
    suspend fun getAggregatedUsage(startTime: Long, endTime: Long): List<AggregatedUsage>
}

data class AggregatedUsage(
    val packageName: String,
    val appName: String,
    val mobileRx: Long,
    val mobileTx: Long,
    val wifiRx: Long,
    val wifiTx: Long
)

@Dao
interface BundleDao {
    @Insert
    suspend fun insertBundle(bundle: BundleEntity)

    @Update
    suspend fun updateBundle(bundle: BundleEntity)

    @Query("SELECT * FROM bundle_info WHERE id = 1")
    suspend fun getBundle(): BundleEntity?
}

@Dao
interface DrainAlertDao {
    @Insert
    suspend fun insertAlert(alert: DrainAlertEntity)

    @Query("SELECT * FROM drain_alerts WHERE timestamp > :since ORDER BY timestamp DESC")
    suspend fun getRecentAlerts(since: Long): List<DrainAlertEntity>

    @Query("DELETE FROM drain_alerts WHERE timestamp < :cutoffTime")
    suspend fun deleteOldAlerts(cutoffTime: Long)
}

@Dao
interface AppTrackingDao {
    @Insert
    suspend fun insertTracking(tracking: AppTrackingEntity): Long

    @Update
    suspend fun updateTracking(tracking: AppTrackingEntity)

    @Query("SELECT * FROM app_tracking WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTracking(): AppTrackingEntity?

    @Query("SELECT * FROM app_tracking WHERE isActive = 0 ORDER BY endTime DESC LIMIT 20")
    suspend fun getCompletedTrackings(): List<AppTrackingEntity>

    @Query("DELETE FROM app_tracking WHERE id = :id")
    suspend fun deleteTracking(id: Int)
}
