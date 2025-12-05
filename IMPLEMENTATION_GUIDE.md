# Data Watchdog - Implementation Guide

## Quick Start

### 1. Clone & Setup
```bash
git clone <repo>
cd DataWatchdog
./gradlew build
```

### 2. Grant Permissions
The app requests these permissions on first launch:
- `PACKAGE_USAGE_STATS` - Read per-app data usage
- `READ_SMS` - Parse bundle expiry from SMS
- `POST_NOTIFICATIONS` - Show alerts

### 3. Start Monitoring
Once permissions are granted, the foreground service starts automatically and:
- Polls data usage every 10 seconds
- Stores data in local database
- Checks for drains and bundle expiry
- Shows notifications

## Architecture Overview

### Data Flow

```
NetworkStatsManager (System API)
    ↓
DataUsageTracker.getAppDataUsage()
    ↓
DataMonitorService (every 10 seconds)
    ├→ DrainDetector.isDraining()
    ├→ SmsParser.parseBundleFromSms()
    └→ AppDatabase.insert()
    ↓
Room Database (Local Storage)
    ↓
ViewModels (UI State)
    ↓
Compose UI (Dashboard/Apps/Bundle)
```

### Component Responsibilities

| Component | Responsibility |
|-----------|-----------------|
| `DataMonitorService` | Continuous monitoring, orchestration |
| `DataUsageTracker` | Query NetworkStatsManager API |
| `DrainDetector` | Detect high-usage apps |
| `SmsParser` | Extract bundle info from SMS |
| `BundlePredictor` | Calculate exhaustion time |
| `AppDatabase` | Persist all data locally |
| `ViewModels` | Manage UI state |
| `Compose UI` | Display data to user |

## Detailed Logic Examples

### Example 1: Real-time Usage Tracking

**Scenario:** User opens YouTube at 10:00 AM

**Timeline:**
```
10:00:00 - Service polls NetworkStatsManager
          YouTube: 0 MB (just started)
          
10:00:10 - Service polls again
          YouTube: 5 MB (buffering video)
          → Recorded in database
          
10:00:20 - Service polls again
          YouTube: 12 MB
          → Recorded in database
          
10:00:30 - Service polls again
          YouTube: 25 MB
          → Recorded in database
          
Dashboard shows: "YouTube: 25 MB today"
```

**Code Flow:**
```kotlin
// In DataMonitorService.monitorDataUsage()
val usageList = tracker.getAppDataUsage() // Query system API

for (usage in usageList) {
    // usage.packageName = "com.google.android.youtube"
    // usage.appName = "YouTube"
    // usage.mobileRx = 25 * 1024 * 1024 bytes
    
    val entity = DataUsageEntity(
        packageName = usage.packageName,
        appName = usage.appName,
        mobileRx = usage.mobileRx,
        mobileTx = usage.mobileTx,
        wifiRx = usage.wifiRx,
        wifiTx = usage.wifiTx,
        timestamp = System.currentTimeMillis(),
        date = "2024-01-15" // Today's date
    )
    
    db.dataUsageDao().insertUsage(entity)
}

// In DashboardViewModel
val today = "2024-01-15"
val topApps = db.dataUsageDao().getTopAppsForDate(today)
// Returns: [YouTube (25MB), Chrome (18MB), Instagram (12MB), ...]
```

### Example 2: Drain Detection

**Scenario:** Rogue app starts consuming 3MB/minute

**Timeline:**
```
10:05:00 - App starts draining
          Recorded: 3 MB
          
10:05:10 - Still draining
          Recorded: 3 MB
          
10:05:20 - Still draining
          Recorded: 3 MB
          
10:05:30 - DrainDetector checks
          Last 3 readings: [3MB, 3MB, 3MB]
          Average: 3 MB/min
          Threshold: 2 MB/min
          → DRAIN DETECTED!
          → Notification shown
          → Alert stored in database
```

**Code Flow:**
```kotlin
// In DataMonitorService.monitorDataUsage()
for (usage in usageList) {
    val totalMobile = usage.getTotalMobile() // 3 * 1024 * 1024 bytes
    
    // Record for drain detection
    drainDetector.recordUsage(usage.packageName, totalMobile)
    // History: [3MB, 3MB, 3MB]
    
    // Check if draining
    if (drainDetector.isDraining(usage.packageName)) {
        val drainRate = drainDetector.getDrainRate(usage.packageName)
        // drainRate = 3.0 MB/min
        
        val alert = DrainAlertEntity(
            packageName = usage.packageName,
            appName = usage.appName,
            drainRate = drainRate,
            timestamp = System.currentTimeMillis()
        )
        db.drainAlertDao().insertAlert(alert)
        
        showDrainNotification(usage.appName, drainRate)
        // Notification: "Rogue App is using 3.00 MB/min"
    }
}

// In DrainDetector.isDraining()
fun isDraining(packageName: String): Boolean {
    val history = usageHistory[packageName] ?: return false
    if (history.size < 3) return false
    
    val recentUsage = history.takeLast(3) // [3MB, 3MB, 3MB]
    val avgUsage = recentUsage.average() // 3.0 MB
    val drainThresholdBytes = 2 * 1024 * 1024 // 2 MB
    
    return avgUsage > drainThresholdBytes // 3.0 > 2.0 = true
}
```

### Example 3: SMS Bundle Parsing

**Scenario:** User receives MTN bundle SMS

**SMS Content:**
```
MTN: Your data bundle is active. 
Valid till 31/01/2024. 
Total: 10GB. 
Used: 2.5GB.
```

**Parsing Flow:**
```kotlin
// In SmsParser.parseBundleFromSms()
val cursor = context.contentResolver.query(
    Telephony.Sms.CONTENT_URI,
    arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE),
    null,
    null,
    "${Telephony.Sms.DATE} DESC LIMIT 50"
)

cursor?.use {
    while (it.moveToNext()) {
        val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
        // body = "MTN: Your data bundle is active..."
        
        val provider = detectProvider(body)
        // provider = "MTN" (found "MTN" in text)
        
        if (provider != null) {
            val expiryDate = parseExpiryDate(body)
            // Pattern: "valid till\\s+([0-9]{1,2}/[0-9]{1,2}/[0-9]{4})"
            // Matches: "31/01/2024"
            // Parsed: 1706745600000 (milliseconds)
            
            val totalMB = parseTotalMB(body)
            // Pattern: "([0-9]+)\\s*(?:MB|GB)"
            // Matches: "10GB"
            // Parsed: 10240 MB
            
            val usedMB = parseUsedMB(body)
            // Pattern: "used\\s+([0-9]+)\\s*(?:MB|GB)"
            // Matches: "2.5GB"
            // Parsed: 2560 MB
            
            return BundleInfo(
                provider = "MTN",
                expiryDate = 1706745600000,
                totalMB = 10240,
                usedMB = 2560
            )
        }
    }
}

// In SmsReceiver.onReceive()
val bundleEntity = BundleEntity(
    expiryDate = 1706745600000,
    totalMB = 10240,
    usedMB = 2560,
    provider = "MTN",
    lastUpdated = System.currentTimeMillis()
)
db.bundleDao().updateBundle(bundleEntity)
```

### Example 4: Bundle Exhaustion Prediction

**Scenario:** User has 500 MB left, using 50 MB/min

**Prediction Flow:**
```kotlin
// In BundlePredictor
usageHistory = [50MB, 50MB, 50MB, 50MB, 50MB, 50MB, 50MB, 50MB, 50MB, 50MB]
// Last 10 minutes of usage

fun predictBundleExhaustionTime(remainingMB: 500): Long {
    val avgBytesPerMinute = usageHistory.average()
    // avgBytesPerMinute = 50 * 1024 * 1024 bytes
    
    val remainingBytes = 500 * 1024 * 1024
    val minutesRemaining = remainingBytes / avgBytesPerMinute
    // minutesRemaining = 500 / 50 = 10 minutes
    
    val millisRemaining = (10 * 60 * 1000).toLong()
    // millisRemaining = 600000 ms
    
    return System.currentTimeMillis() + 600000
    // Returns: now + 10 minutes
}

// In BundleScreen UI
val exhaustDate = SimpleDateFormat("MMM dd, HH:mm").format(Date(exhaustionTime))
// Shows: "Jan 31, 14:30" (10 minutes from now)
```

### Example 5: Dashboard Data Aggregation

**Scenario:** User opens Dashboard at 10:00 AM

**Data Aggregation:**
```kotlin
// In DashboardViewModel.loadData()
val today = "2024-01-15"

// Get total mobile usage
val mobileUsage = db.dataUsageDao().getTotalMobileUsageForDate(today)
// Query: SELECT SUM(mobileRx + mobileTx) FROM data_usage WHERE date = '2024-01-15'
// Result: 1500 * 1024 * 1024 bytes = 1500 MB

_totalMobileUsage.value = tracker.bytesToMB(mobileUsage)
// _totalMobileUsage = 1500.0

// Get total WiFi usage
val wifiUsage = db.dataUsageDao().getTotalWifiUsageForDate(today)
// Result: 500 * 1024 * 1024 bytes = 500 MB

_totalWifiUsage.value = tracker.bytesToMB(wifiUsage)
// _totalWifiUsage = 500.0

// Get top 5 apps
val topAppsList = db.dataUsageDao().getTopAppsForDate(today)
// Query: SELECT * FROM data_usage WHERE date = '2024-01-15'
//        ORDER BY (mobileRx + mobileTx + wifiRx + wifiTx) DESC LIMIT 5
// Results:
// 1. YouTube: 500 MB
// 2. Chrome: 400 MB
// 3. Instagram: 300 MB
// 4. WhatsApp: 200 MB
// 5. Spotify: 100 MB

_topApps.value = topAppsList

// Get bundle info
val bundle = db.bundleDao().getBundle()
// Result: BundleEntity(
//   provider = "MTN",
//   expiryDate = 1706745600000,
//   totalMB = 10240,
//   usedMB = 2560,
//   lastUpdated = 1705334400000
// )

_bundleInfo.value = bundle
```

**UI Rendering:**
```
┌─────────────────────────────────┐
│ Data Watchdog                   │
├─────────────────────────────────┤
│ Mobile: 1500.00 MB │ WiFi: 500.00 MB │
├─────────────────────────────────┤
│ Bundle: MTN                     │
│ 2560/10240 MB                   │
│ Expires in 16 hours             │
├─────────────────────────────────┤
│ Top 5 Apps                      │
│ 1. YouTube: 500 MB              │
│ 2. Chrome: 400 MB               │
│ 3. Instagram: 300 MB            │
│ 4. WhatsApp: 200 MB             │
│ 5. Spotify: 100 MB              │
└─────────────────────────────────┘
```

## Testing Scenarios

### Test 1: Verify Data Tracking
```bash
# Open app
# Use YouTube for 5 minutes
# Check Dashboard
# Expected: YouTube shows ~100-200 MB (depends on video quality)
```

### Test 2: Verify Drain Detection
```bash
# Open app
# Start large file download
# Wait 30 seconds
# Expected: Notification "App is using X MB/min"
```

### Test 3: Verify SMS Parsing
```bash
# Send test SMS: "MTN: Valid till 31/01/2024. 10GB total. Used 2.5GB"
# Open Bundle screen
# Expected: Shows "MTN", "2560/10240 MB", expiry countdown
```

### Test 4: Verify Prediction
```bash
# Open Bundle screen
# Expected: Shows "At current usage rate: X MB/min"
# Expected: Shows "Bundle will exhaust: [date/time]"
```

## Troubleshooting

### Issue: No data showing in Dashboard
**Solution:**
1. Check permissions: Settings → Apps → Data Watchdog → Permissions
2. Ensure PACKAGE_USAGE_STATS is granted
3. Wait 10 seconds for first data poll
4. Use some data (open YouTube, etc.)

### Issue: Drain alerts not showing
**Solution:**
1. Check notification permissions
2. Ensure app is using > 2MB/minute
3. Check logcat: `adb logcat | grep DataWatchdog`

### Issue: Bundle info not updating
**Solution:**
1. Check SMS read permission
2. Ensure SMS contains provider name (MTN/Vodafone/AirtelTigo)
3. Check SMS format matches regex patterns
4. Manually trigger SMS parsing in SmsReceiver

### Issue: App crashes on startup
**Solution:**
1. Check logcat for stack trace
2. Ensure all permissions are granted
3. Clear app data: `adb shell pm clear com.datawatchdog`
4. Rebuild and reinstall

## Performance Optimization

### Memory Usage
- Database queries are paginated (LIMIT 5 for top apps)
- Old data deleted after 30 days
- Usage history limited to 10 entries per app

### Battery Usage
- 10-second polling interval (configurable)
- Foreground service keeps app alive
- No background wake-locks

### Storage Usage
- Room database auto-compacts
- Old records deleted periodically
- Typical: 10-50 MB for 1 month of data

## Future Enhancements

1. **Weekly Reports**
   - Aggregate usage by week
   - Show trends and patterns

2. **Custom Alerts**
   - User-configurable drain threshold
   - Per-app usage limits

3. **Data Saver Mode**
   - Recommend apps to restrict
   - Show potential savings

4. **Export Data**
   - CSV export of usage history
   - Share reports

5. **Multiple Bundles**
   - Track multiple active bundles
   - Separate mobile/WiFi limits

## Code Quality

- **No external dependencies** (except AndroidX)
- **100% Kotlin** (no Java)
- **Coroutines** for async operations
- **Room** for type-safe database access
- **Compose** for modern UI
- **MVVM** architecture for testability

## License

MIT - Free to use and modify
