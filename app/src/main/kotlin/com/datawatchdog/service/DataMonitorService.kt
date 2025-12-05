package com.datawatchdog.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.datawatchdog.R
import com.datawatchdog.db.AppDatabase
import com.datawatchdog.db.DataUsageEntity
import com.datawatchdog.db.DrainAlertEntity
import com.datawatchdog.util.DataUsageTracker
import com.datawatchdog.util.DrainDetector
import com.datawatchdog.util.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DataMonitorService : Service() {
    private lateinit var db: AppDatabase
    private lateinit var tracker: DataUsageTracker
    private lateinit var drainDetector: DrainDetector
    private lateinit var smsParser: SmsParser
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastMobileUsage = 0L
    private var lastWifiUsage = 0L

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getDatabase(this)
        tracker = DataUsageTracker(this)
        drainDetector = DrainDetector()
        smsParser = SmsParser(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                scope.launch {
                    monitorDataUsage()
                    checkBundleExpiry()
                }
                handler.postDelayed(this, MONITOR_INTERVAL)
            }
        }, MONITOR_INTERVAL)
    }

    private suspend fun monitorDataUsage() {
        try {
            val usageList = tracker.getAppDataUsage()
            val today = tracker.getTodayDate()

            for (usage in usageList) {
                val totalMobile = usage.getTotalMobile()
                val totalWifi = usage.getTotalWifi()

                drainDetector.recordUsage(usage.packageName, totalMobile)

                if (drainDetector.isDraining(usage.packageName)) {
                    val drainRate = drainDetector.getDrainRate(usage.packageName)
                    val alert = DrainAlertEntity(
                        packageName = usage.packageName,
                        appName = usage.appName,
                        drainRate = drainRate,
                        timestamp = System.currentTimeMillis()
                    )
                    db.drainAlertDao().insertAlert(alert)
                    showDrainNotification(usage.appName, drainRate)
                }

                val entity = DataUsageEntity(
                    packageName = usage.packageName,
                    appName = usage.appName,
                    mobileRx = usage.mobileRx,
                    mobileTx = usage.mobileTx,
                    wifiRx = usage.wifiRx,
                    wifiTx = usage.wifiTx,
                    timestamp = System.currentTimeMillis(),
                    date = today
                )
                db.dataUsageDao().insertUsage(entity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun checkBundleExpiry() {
        try {
            val bundle = smsParser.parseBundleFromSms()
            if (bundle != null) {
                val now = System.currentTimeMillis()
                val hoursUntilExpiry = (bundle.expiryDate - now) / (1000 * 60 * 60)

                if (hoursUntilExpiry < 24 && hoursUntilExpiry > 0) {
                    showBundleExpiryNotification(hoursUntilExpiry.toInt())
                }

                val bundleEntity = com.datawatchdog.db.BundleEntity(
                    expiryDate = bundle.expiryDate,
                    totalMB = bundle.totalMB,
                    usedMB = bundle.usedMB,
                    provider = bundle.provider,
                    lastUpdated = now
                )
                db.bundleDao().updateBundle(bundleEntity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showDrainNotification(appName: String, drainRate: Double) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("High Data Drain Detected")
            .setContentText("$appName is using ${String.format("%.2f", drainRate)} MB/min")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(DRAIN_NOTIFICATION_ID, notification)
    }

    private fun showBundleExpiryNotification(hoursRemaining: Int) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Bundle Expiring Soon")
            .setContentText("Your data bundle expires in $hoursRemaining hours")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(EXPIRY_NOTIFICATION_ID, notification)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Data Watchdog")
            .setContentText("Monitoring data usage...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Data Monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val DRAIN_NOTIFICATION_ID = 2
        private const val EXPIRY_NOTIFICATION_ID = 3
        private const val CHANNEL_ID = "data_monitoring"
        private const val MONITOR_INTERVAL = 10000L // 10 seconds
    }
}
