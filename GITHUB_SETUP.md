# GitHub Setup & Automated Builds

## Quick Setup (2 minutes)

### 1. Create GitHub Repository

```bash
# Initialize git
git init
git add .
git commit -m "Initial commit: Data Watchdog MVP"

# Create repo on GitHub, then:
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/DataWatchdog.git
git push -u origin main
```

### 2. Enable GitHub Actions

- Go to your repository on GitHub
- Click **Settings** → **Actions** → **General**
- Select "Allow all actions and reusable workflows"
- Click **Save**

### 3. First Build

- Push code to main branch
- Go to **Actions** tab
- Watch the workflow run
- Download APK when complete

## Download APK

### From GitHub Actions

1. Go to your repository
2. Click **Actions** tab
3. Click the latest workflow run (green checkmark)
4. Scroll to **Artifacts** section
5. Click **DataWatchdog-APK** to download
6. Extract and install: `adb install app-debug.apk`

## Workflow Details

**File**: `.github/workflows/build.yml`

**Triggers**:
- Push to main branch
- Pull requests to main
- Manual trigger (workflow_dispatch)

**Build Time**: 2-10 minutes (first build slower)

**Output**: `build/outputs/apk/debug/app-debug.apk`

## What's Included

✅ Automatic builds on every push
✅ APK download from GitHub
✅ No local build required
✅ CI/CD pipeline ready
✅ Artifact retention (90 days)

## Next Steps

1. Create GitHub repository
2. Push code
3. Enable Actions
4. Download APK
5. Install on device

## Troubleshooting

**Build fails?**
- Check Actions tab for error logs
- Ensure Java 17 is available
- Verify Gradle configuration

**APK not found?**
- Wait for build to complete
- Check if build succeeded (green checkmark)
- Refresh page

**Slow builds?**
- First build: 5-10 minutes
- Subsequent builds: 2-3 minutes
- Gradle caches dependencies

## Documentation

See **GITHUB_ACTIONS.md** for:
- Detailed workflow configuration
- Customization options
- Advanced features
- Security notes
