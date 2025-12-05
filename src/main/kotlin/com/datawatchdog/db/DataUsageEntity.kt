package com.datawatchdog.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "data_usage")
data class DataUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val packageName: String,
    val appName: String,
    val mobileRx: Long,
    val mobileTx: Long,
    val wifiRx: Long,
    val wifiTx: Long,
    val timestamp: Long,
    val date: String
)

@Entity(tableName = "bundle_info")
data class BundleEntity(
    @PrimaryKey
    val id: Int = 1,
    val expiryDate: Long,
    val totalMB: Long,
    val usedMB: Long,
    val provider: String,
    val lastUpdated: Long
)

@Entity(tableName = "drain_alerts")
data class DrainAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val packageName: String,
    val appName: String,
    val drainRate: Double,
    val timestamp: Long
)
