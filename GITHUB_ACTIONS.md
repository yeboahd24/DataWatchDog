# GitHub Actions - Automated APK Build

## Overview

The project includes a GitHub Actions workflow that automatically builds the Android APK whenever you push code or create a pull request.

## Workflow File

Location: `.github/workflows/build.yml`

## How It Works

### Triggers

The workflow runs automatically on:
- **Push to main branch** - Every commit to main
- **Pull requests to main** - Every PR to main
- **Manual trigger** - Click "Run workflow" in GitHub Actions tab

### Build Steps

1. **Checkout code** - Clones your repository
2. **Setup JDK 17** - Installs Java 17 (required for Gradle)
3. **Grant permissions** - Makes gradlew executable
4. **Build APK** - Runs `./gradlew assembleDebug`
5. **Upload artifact** - Stores APK for download

## Download APK

### From GitHub Actions

1. Go to your repository on GitHub
2. Click **Actions** tab
3. Click the latest workflow run
4. Scroll down to **Artifacts** section
5. Click **DataWatchdog-APK** to download

### APK Location

The APK is built at: `build/outputs/apk/debug/app-debug.apk`

## Setup Instructions

### 1. Push to GitHub

```bash
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/DataWatchdog.git
git push -u origin main
```

### 2. Enable Actions

- Go to your repository on GitHub
- Click **Settings** → **Actions** → **General**
- Ensure "Allow all actions and reusable workflows" is selected

### 3. First Build

- Push code to main branch
- Go to **Actions** tab
- Watch the workflow run
- Download APK when complete

## Workflow Configuration

```yaml
name: Build Android APK

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          distribution: "temurin"
          java-version: "17"
      - run: chmod +x ./gradlew
      - run: ./gradlew assembleDebug
      - uses: actions/upload-artifact@v3
        with:
          name: DataWatchdog-APK
          path: build/outputs/apk/debug/app-debug.apk
```

## Customization

### Change Build Type

To build release APK instead of debug:

```yaml
- name: Build APK
  run: ./gradlew assembleRelease
```

### Change Java Version

To use Java 11 instead of 17:

```yaml
- uses: actions/setup-java@v3
  with:
    distribution: "temurin"
    java-version: "11"
```

### Add More Triggers

To also trigger on tags:

```yaml
on:
  push:
    branches: [ "main" ]
    tags: [ "v*" ]
  pull_request:
    branches: [ "main" ]
  workflow_dispatch:
```

## Troubleshooting

### Build Fails

1. Check the workflow logs in GitHub Actions
2. Look for error messages
3. Common issues:
   - Missing permissions (check Settings → Actions)
   - Gradle cache issues (clear cache in workflow)
   - Java version mismatch

### APK Not Found

- Ensure `build/outputs/apk/debug/app-debug.apk` path is correct
- Check if build succeeded (no errors in logs)
- Verify Gradle configuration

### Slow Builds

- GitHub Actions caches Gradle dependencies
- First build is slower (~5-10 minutes)
- Subsequent builds are faster (~2-3 minutes)

## Advanced Features

### Cache Dependencies

Add to workflow to speed up builds:

```yaml
- uses: actions/cache@v3
  with:
    path: ~/.gradle/caches
    key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}
    restore-keys: |
      ${{ runner.os }}-gradle-
```

### Notify on Failure

Add to workflow to get notifications:

```yaml
- name: Notify on failure
  if: failure()
  run: echo "Build failed!"
```

### Build Multiple Variants

```yaml
- name: Build Debug APK
  run: ./gradlew assembleDebug

- name: Build Release APK
  run: ./gradlew assembleRelease
```

## Security Notes

- Debug APK is unsigned (for testing only)
- For production, sign the APK with your keystore
- Never commit keystore files to GitHub
- Use GitHub Secrets for sensitive data

## Next Steps

1. Push code to GitHub
2. Go to Actions tab
3. Watch the build
4. Download APK
5. Install on device: `adb install app-debug.apk`

## Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android Gradle Plugin](https://developer.android.com/studio/build)
- [Gradle Build Tool](https://gradle.org/)

## Support

For issues with GitHub Actions:
1. Check workflow logs in GitHub Actions tab
2. Review this documentation
3. Check GitHub Actions documentation
4. Open an issue on GitHub
