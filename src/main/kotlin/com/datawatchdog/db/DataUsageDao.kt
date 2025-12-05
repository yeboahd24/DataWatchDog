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
}

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
