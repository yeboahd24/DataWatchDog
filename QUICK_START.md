# Data Watchdog - Quick Start Guide

## 5-Minute Setup

### 1. Build the App
```bash
cd DataWatchdog
./gradlew build
```

### 2. Install on Device
```bash
./gradlew installDebug
```

### 3. Grant Permissions
```bash
adb shell pm grant com.datawatchdog android.permission.PACKAGE_USAGE_STATS
adb shell pm grant com.datawatchdog android.permission.READ_SMS
adb shell pm grant com.datawatchdog android.permission.POST_NOTIFICATIONS
```

### 4. Launch App
- Open "Data Watchdog" from app drawer
- Tap "Dashboard" to see usage
- Wait 10 seconds for first data poll

## What You'll See

### Dashboard Tab
- **Mobile**: Total mobile data used today
- **WiFi**: Total WiFi data used today
- **Top 5 Apps**: Apps consuming most data
- **Bundle**: Current provider, expiry countdown

### Apps Tab
- All apps sorted by total usage
- Mobile vs WiFi breakdown for each app
- Real-time updates

### Bundle Tab
- Bundle provider name
- Usage progress bar
- Expiry date and countdown
- Prediction: When bundle will run out
- Average usage rate

## How It Works

### Real-time Tracking
- Service polls every 10 seconds
- Queries system NetworkStatsManager
- Stores data in local database
- No internet required

### Drain Detection
- Monitors last 10 minutes of usage
- Alerts if app uses > 2MB/minute
- Shows notification with drain rate

### Bundle Parsing
- Reads SMS messages automatically
- Extracts expiry date from text
- Supports MTN, Vodafone, AirtelTigo
- Updates when SMS arrives

### Exhaustion Prediction
- Calculates average usage rate
- Predicts exact exhaustion time
- Updates every 10 seconds

## Example Scenarios

### Scenario 1: Check Today's Usage
1. Open app
2. Go to Dashboard
3. See total mobile/WiFi usage
4. See top 5 apps

### Scenario 2: Find Data Hog
1. Go to Apps tab
2. Scroll to find app with high usage
3. See mobile vs WiFi breakdown
4. Identify which type of data it uses

### Scenario 3: Check Bundle Status
1. Go to Bundle tab
2. See usage progress bar
3. See expiry countdown
4. See when bundle will run out

### Scenario 4: Get Drain Alert
1. Open app that uses lots of data
2. Wait 30 seconds
3. Notification appears: "App is using X MB/min"
4. Check drain alerts in database

## Permissions Explained

| Permission | Why Needed |
|-----------|-----------|
| PACKAGE_USAGE_STATS | Read per-app data usage from system |
| READ_SMS | Parse bundle expiry from SMS messages |
| POST_NOTIFICATIONS | Show alerts and notifications |
| FOREGROUND_SERVICE | Keep monitoring service running |

## Troubleshooting

### No data showing?
- Wait 10 seconds after opening app
- Use some data (open YouTube, etc.)
- Check permissions are granted

### Drain alerts not showing?
- Use app that consumes > 2MB/minute
- Wait 30 seconds for detection
- Check notification settings

### Bundle info not showing?
- Receive SMS from MTN/Vodafone/AirtelTigo
- SMS must contain provider name and date
- Check SMS format matches patterns

### App crashes?
- Clear app data: `adb shell pm clear com.datawatchdog`
- Rebuild: `./gradlew clean build`
- Check logcat: `adb logcat | grep DataWatchdog`

## File Locations

| File | Purpose |
|------|---------|
| MainActivity.kt | Main app entry point |
| DataMonitorService.kt | Background monitoring |
| DataUsageTracker.kt | Query system API |
| SmsParser.kt | Parse SMS messages |
| DrainDetector.kt | Detect high usage |
| BundlePredictor.kt | Predict exhaustion |
| AppDatabase.kt | Local storage |

## Key Metrics

- **Update Interval**: 10 seconds
- **Drain Threshold**: 2 MB/minute
- **History Window**: 10 minutes
- **Top Apps**: 5 apps
- **Storage**: ~10-50 MB per month

## Next Steps

1. **Read README.md** for full documentation
2. **Read IMPLEMENTATION_GUIDE.md** for detailed logic
3. **Read PROJECT_SUMMARY.md** for file structure
4. **Explore code** in src/main/kotlin/

## Common Questions

**Q: Does it work offline?**
A: Yes, 100% offline. No internet required.

**Q: Does it drain battery?**
A: ~2-5% per hour (foreground service).

**Q: Where is data stored?**
A: Local Room database on device.

**Q: Can I export data?**
A: Not yet, but can be added.

**Q: Does it track WiFi?**
A: Yes, both mobile and WiFi.

**Q: Can I set custom alerts?**
A: Not yet, but threshold is configurable.

**Q: Does it work with all carriers?**
A: SMS parsing works for MTN, Vodafone, AirtelTigo.

**Q: Can I track multiple bundles?**
A: Not yet, but can be added.

## Support

- Check README.md for full documentation
- Check IMPLEMENTATION_GUIDE.md for examples
- Check PROJECT_SUMMARY.md for architecture
- Check logcat for errors: `adb logcat | grep DataWatchdog`

## License

MIT - Free to use and modify
