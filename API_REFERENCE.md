# Data Watchdog - API Reference

## Core Classes

### DataUsageTracker

Queries system NetworkStatsManager for per-app data usage.

```kotlin
class DataUsageTracker(context: Context)

// Get all apps' data usage for last 24 hours
fun getAppDataUsage(): List<AppDataUsage>

// Get today's date in "yyyy-MM-dd" format
fun getTodayDate(): String

// Convert bytes to MB
fun bytesToMB(bytes: Long): Double
```

**AppDataUsage Data Class**
```kotlin
data class AppDataUsage(
    val packageName: String,      // e.g., "com.google.android.youtube"
    val appName: String,          // e.g., "YouTube"
    val mobileRx: Long,           // Mobile received bytes
    val mobileTx: Long,           // Mobile transmitted bytes
    val wifiRx: Long,             // WiFi received bytes
    val wifiTx: Long              // WiFi transmitted bytes
)

// Helper methods
fun getTotalMobile(): Long        // mobileRx + mobileTx
fun getTotalWifi(): Long          // wifiRx + wifiTx
fun getTotal(): Long              // All data combined
```

**Example Usage**
```kotlin
val tracker = DataUsageTracker(context)
val apps = tracker.getAppDataUsage()

for (app in apps) {
    println("${app.appName}: ${tracker.bytesToMB(app.getTotal())} MB")
}
```

---

### SmsParser

Parses bundle information from SMS messages.

```kotlin
class SmsParser(context: Context)

// Parse bundle info from recent SMS messages
fun parseBundleFromSms(): BundleInfo?

// Detect provider from SMS text
private fun detectProvider(text: String): String?  // "MTN", "Vodafone", "AirtelTigo"

// Extract expiry date from SMS text
private fun parseExpiryDate(text: String): Long?   // Milliseconds

// Extract total MB from SMS text
private fun parseTotalMB(text: String): Long       // MB

// Extract used MB from SMS text
private fun parseUsedMB(text: String): Long        // MB
```

**BundleInfo Data Class**
```kotlin
data class BundleInfo(
    val provider: String,         // "MTN", "Vodafone", "AirtelTigo"
    val expiryDate: Long,         // Milliseconds since epoch
    val totalMB: Long,            // Total bundle size in MB
    val usedMB: Long              // Used amount in MB
)
```

**Supported SMS Patterns**
```
Provider Detection:
- "MTN" → "MTN"
- "Vodafone" → "Vodafone"
- "AirtelTigo" → "AirtelTigo"

Expiry Date Patterns:
- "valid till 31/01/2024"
- "expires on 31-01-2024"
- "expired 31/01/2024"
- "valid until 31/01/2024"

Data Amount Patterns:
- "10GB" → 10240 MB
- "500MB" → 500 MB
- "used 2.5GB" → 2560 MB
```

**Example Usage**
```kotlin
val parser = SmsParser(context)
val bundle = parser.parseBundleFromSms()

if (bundle != null) {
    println("Provider: ${bundle.provider}")
    println("Expiry: ${Date(bundle.expiryDate)}")
    println("Used: ${bundle.usedMB}/${bundle.totalMB} MB")
}
```

---

### DrainDetector

Detects apps consuming unusually high data.

```kotlin
class DrainDetector

// Record usage for an app (call every 10 seconds)
fun recordUsage(packageName: String, bytesUsed: Long)

// Check if app is draining (> 2MB/minute)
fun isDraining(packageName: String): Boolean

// Get drain rate in MB/minute
fun getDrainRate(packageName: String): Double

// Clear history for specific app
fun clearHistory(packageName: String)

// Clear all history
fun clearAllHistory()
```

**Algorithm**
```
1. Maintain rolling window of last 10 usage readings
2. Calculate average of last 3 readings
3. If average > 2MB/minute → isDraining = true
4. Return average as drain rate
```

**Example Usage**
```kotlin
val detector = DrainDetector()

// Every 10 seconds:
detector.recordUsage("com.example.app", 3_000_000) // 3 MB

// Check if draining
if (detector.isDraining("com.example.app")) {
    val rate = detector.getDrainRate("com.example.app")
    println("App is draining at $rate MB/min")
}
```

---

### BundlePredictor

Predicts when data bundle will be exhausted.

```kotlin
class BundlePredictor

// Record usage (call every 10 seconds)
fun recordUsage(bytesUsed: Long)

// Predict exhaustion time in milliseconds
fun predictBundleExhaustionTime(remainingMB: Long): Long

// Get average usage per minute in MB
fun getAverageUsagePerMinute(): Double

// Clear history
fun clear()
```

**Algorithm**
```
1. Maintain rolling window of last 10 usage readings
2. Calculate average bytes per minute
3. Calculate: minutesRemaining = remainingBytes / avgBytesPerMinute
4. Return: now + (minutesRemaining * 60 * 1000)
```

**Example Usage**
```kotlin
val predictor = BundlePredictor()

// Every 10 seconds:
predictor.recordUsage(50_000_000) // 50 MB

// Predict exhaustion
val exhaustionTime = predictor.predictBundleExhaustionTime(500) // 500 MB remaining
val date = SimpleDateFormat("HH:mm").format(Date(exhaustionTime))
println("Bundle will exhaust at $date")
```

---

## Database Classes

### AppDatabase

Room database singleton for local storage.

```kotlin
@Database(
    entities = [DataUsageEntity::class, BundleEntity::class, DrainAlertEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase()

// Get database instance (thread-safe singleton)
companion object {
    fun getDatabase(context: Context): AppDatabase
}

// Access DAOs
abstract fun dataUsageDao(): DataUsageDao
abstract fun bundleDao(): BundleDao
abstract fun drainAlertDao(): DrainAlertDao
```

**Example Usage**
```kotlin
val db = AppDatabase.getDatabase(context)
val usageDao = db.dataUsageDao()
val bundleDao = db.bundleDao()
val alertDao = db.drainAlertDao()
```

---

### DataUsageDao

Database access for usage data.

```kotlin
@Dao
interface DataUsageDao {
    // Insert usage record
    suspend fun insertUsage(usage: DataUsageEntity)
    
    // Get top 5 apps for date
    suspend fun getTopAppsForDate(date: String): List<DataUsageEntity>
    
    // Get all apps for date
    suspend fun getUsageForDate(date: String): List<DataUsageEntity>
    
    // Get total mobile usage for date
    suspend fun getTotalMobileUsageForDate(date: String): Long?
    
    // Get total WiFi usage for date
    suspend fun getTotalWifiUsageForDate(date: String): Long?
    
    // Delete old data before cutoff time
    suspend fun deleteOldData(cutoffTime: Long)
}
```

**Example Usage**
```kotlin
val dao = db.dataUsageDao()

// Insert
val entity = DataUsageEntity(
    packageName = "com.example.app",
    appName = "Example App",
    mobileRx = 1000000,
    mobileTx = 500000,
    wifiRx = 2000000,
    wifiTx = 1000000,
    timestamp = System.currentTimeMillis(),
    date = "2024-01-15"
)
dao.insertUsage(entity)

// Query
val topApps = dao.getTopAppsForDate("2024-01-15")
val totalMobile = dao.getTotalMobileUsageForDate("2024-01-15") ?: 0L
```

---

### BundleDao

Database access for bundle information.

```kotlin
@Dao
interface BundleDao {
    // Insert bundle
    suspend fun insertBundle(bundle: BundleEntity)
    
    // Update bundle
    suspend fun updateBundle(bundle: BundleEntity)
    
    // Get current bundle
    suspend fun getBundle(): BundleEntity?
}
```

**Example Usage**
```kotlin
val dao = db.bundleDao()

// Insert/Update
val bundle = BundleEntity(
    expiryDate = 1706745600000,
    totalMB = 10240,
    usedMB = 2560,
    provider = "MTN",
    lastUpdated = System.currentTimeMillis()
)
dao.updateBundle(bundle)

// Query
val current = dao.getBundle()
```

---

### DrainAlertDao

Database access for drain alerts.

```kotlin
@Dao
interface DrainAlertDao {
    // Insert alert
    suspend fun insertAlert(alert: DrainAlertEntity)
    
    // Get recent alerts
    suspend fun getRecentAlerts(since: Long): List<DrainAlertEntity>
    
    // Delete old alerts
    suspend fun deleteOldAlerts(cutoffTime: Long)
}
```

---

## Service Classes

### DataMonitorService

Foreground service for continuous monitoring.

```kotlin
class DataMonitorService : Service()

// Called when service starts
override fun onCreate()

// Called when service is started
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int

// Start monitoring loop (10-second interval)
private fun startMonitoring()

// Monitor data usage and detect drains
private suspend fun monitorDataUsage()

// Check bundle expiry
private suspend fun checkBundleExpiry()

// Show drain notification
private fun showDrainNotification(appName: String, drainRate: Double)

// Show bundle expiry notification
private fun showBundleExpiryNotification(hoursRemaining: Int)
```

**Starting the Service**
```kotlin
val intent = Intent(context, DataMonitorService::class.java)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    context.startForegroundService(intent)
} else {
    context.startService(intent)
}
```

---

### SmsReceiver

Broadcast receiver for SMS messages.

```kotlin
class SmsReceiver : BroadcastReceiver()

// Called when SMS is received
override fun onReceive(context: Context?, intent: Intent?)
```

**Manifest Declaration**
```xml
<receiver
    android:name=".receiver.SmsReceiver"
    android:exported="true"
    android:permission="android.permission.BROADCAST_SMS">
    <intent-filter>
        <action android:name="android.provider.Telephony.SMS_RECEIVED" />
    </intent-filter>
</receiver>
```

---

## ViewModel Classes

### DashboardViewModel

Manages dashboard UI state.

```kotlin
class DashboardViewModel(context: Context) : ViewModel()

// Total mobile usage today (MB)
val totalMobileUsage: StateFlow<Double>

// Total WiFi usage today (MB)
val totalWifiUsage: StateFlow<Double>

// Top 5 apps
val topApps: StateFlow<List<AppDataUsage>>

// Current bundle info
val bundleInfo: StateFlow<BundleEntity?>

// Refresh data
fun refresh()
```

**Example Usage**
```kotlin
val viewModel = DashboardViewModel(context)

// Collect state
val mobile = viewModel.totalMobileUsage.collectAsState()
val topApps = viewModel.topApps.collectAsState()

// Refresh
viewModel.refresh()
```

---

### AppListViewModel

Manages app list UI state.

```kotlin
class AppListViewModel(context: Context) : ViewModel()

// All apps sorted by usage
val allApps: StateFlow<List<AppDataUsage>>

// Refresh data
fun refresh()
```

---

### BundleViewModel

Manages bundle UI state.

```kotlin
class BundleViewModel(context: Context) : ViewModel()

// Current bundle info
val bundleInfo: StateFlow<BundleEntity?>

// Exhaustion prediction
val exhaustionPrediction: StateFlow<PredictionData?>

// Refresh data
fun refresh()
```

**PredictionData**
```kotlin
data class PredictionData(
    val exhaustionTime: Long,      // Milliseconds
    val avgUsagePerMinute: Double  // MB/min
)
```

---

## UI Composables

### DashboardScreen

```kotlin
@Composable
fun DashboardScreen(viewModel: DashboardViewModel)
```

Displays:
- Total mobile/WiFi usage
- Top 5 apps
- Bundle info with countdown

---

### AppListScreen

```kotlin
@Composable
fun AppListScreen(viewModel: AppListViewModel)
```

Displays:
- All apps sorted by usage
- Mobile vs WiFi breakdown
- Real-time updates

---

### BundleScreen

```kotlin
@Composable
fun BundleScreen(viewModel: BundleViewModel)
```

Displays:
- Bundle provider and progress
- Expiry date/time
- Exhaustion prediction
- Average usage rate

---

## Constants

```kotlin
// DataMonitorService
const val NOTIFICATION_ID = 1
const val DRAIN_NOTIFICATION_ID = 2
const val EXPIRY_NOTIFICATION_ID = 3
const val CHANNEL_ID = "data_monitoring"
const val MONITOR_INTERVAL = 10000L  // 10 seconds

// DrainDetector
const val DRAIN_THRESHOLD_BYTES = 2 * 1024 * 1024  // 2 MB/minute
const val MAX_HISTORY_SIZE = 10  // Last 10 minutes

// BundlePredictor
const val MAX_HISTORY_SIZE = 10  // Last 10 minutes
```

---

## Error Handling

All database operations are suspend functions and should be called from coroutines:

```kotlin
viewModelScope.launch {
    try {
        val data = db.dataUsageDao().getTopAppsForDate(date)
        // Process data
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
```

---

## Thread Safety

- **Database**: Thread-safe (Room handles it)
- **Singleton**: Thread-safe (synchronized block)
- **Coroutines**: All DB operations on IO dispatcher
- **UI**: All UI updates on Main dispatcher

---

## Performance Tips

1. **Limit queries**: Use LIMIT clause
2. **Delete old data**: Call deleteOldData() periodically
3. **Batch inserts**: Insert multiple records at once
4. **Use indexes**: Date column is indexed
5. **Avoid large queries**: Query by date, not all time

---

## Debugging

Enable logging:
```kotlin
// In DataMonitorService
Log.d("DataWatchdog", "Usage: ${usage.appName} = ${usage.getTotal()} bytes")

// In SmsParser
Log.d("DataWatchdog", "Bundle: ${bundle.provider} expires ${bundle.expiryDate}")

// In DrainDetector
Log.d("DataWatchdog", "Drain detected: ${packageName} at ${drainRate} MB/min")
```

View logs:
```bash
adb logcat | grep DataWatchdog
```

---

## License

MIT - Free to use and modify
