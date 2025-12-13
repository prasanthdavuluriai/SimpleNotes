# Build Instructions for SimpleNotes Android App

## Prerequisites

Before building the SimpleNotes app, ensure you have the following installed:

1. **Java Development Kit (JDK)**
   - JDK 8 or higher
   - JAVA_HOME environment variable set

2. **Android SDK**
   - Android SDK API Level 21 (Android 5.0) or higher
   - Android SDK Build-Tools
   - Android SDK Platform-Tools
   - Android Emulator (optional, for testing)

3. **Android Studio** (Recommended)
   - Download from: https://developer.android.com/studio
   - Includes all necessary SDK components

## Build Commands

### Using Gradle Wrapper (Recommended)

1. **Debug APK**
   ```bash
   cd SimpleNotes
   ./gradlew assembleDebug
   ```
   Output: `app/build/outputs/apk/debug/app-debug.apk`

2. **Release APK** 
   ```bash
   cd SimpleNotes
   ./gradlew assembleRelease
   ```
   Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

3. **Android App Bundle (AAB)** - For Google Play Store
   ```bash
   cd SimpleNotes
   ./gradlew bundleRelease
   ```
   Output: `app/build/outputs/bundle/release/app-release.aab`

### Using Local Gradle Installation

If you have Gradle installed locally:
```bash
cd SimpleNotes
gradle assembleDebug
gradle assembleRelease
gradle bundleRelease
```

## APK/AAB Locations

After successful builds, your files will be located in:

- **Debug APK**: `SimpleNotes/app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `SimpleNotes/app/build/outputs/apk/release/app-release-unsigned.apk`
- **AAB Bundle**: `SimpleNotes/app/build/outputs/bundle/release/app-release.aab`

## Signing Release APK

For production deployment, you need to sign your APK:

1. **Generate Keystore** (one-time setup):
   ```bash
   keytool -genkey -v -keystore simplenotes-release-key.keystore -alias simplenotes-key -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Configure Signing** in `app/build.gradle`:
   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file('simplenotes-release-key.keystore')
               storePassword 'your_store_password'
               keyAlias 'simplenotes-key'
               keyPassword 'your_key_password'
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
           }
       }
   }
   ```

3. **Build Signed Release APK**:
   ```bash
   ./gradlew assembleRelease
   ```

## Installation and Testing

### Install on Device/Emulator
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Test App Features
1. Launch the app from your device
2. Tap the "+" button to add a new note
3. Enter title and content
4. Tap "Save" to return to notes list
5. Tap any note to edit it

## Troubleshooting

### Common Build Issues

1. **"SDK location not found"**
   - Set ANDROID_HOME environment variable
   - Or create `local.properties` file with `sdk.dir=/path/to/android/sdk`

2. **"Gradle sync failed"**
   - Check internet connection
   - Update Gradle wrapper: `./gradlew wrapper --gradle-version 8.0`

3. **"Build Tools version not found"**
   - Install required build tools via Android SDK Manager
   - Update build.gradle SDK versions if needed

4. **"Java version mismatch"**
   - Ensure JAVA_HOME points to JDK 8+
   - Check with `java -version`

### Performance Optimization

1. **Enable Build Cache**:
   Add to `gradle.properties`:
   ```
   org.gradle.caching=true
   org.gradle.parallel=true
   ```

2. **Increase Memory**:
   Add to `gradle.properties`:
   ```
   org.gradle.jvmargs=-Xmx4g -XX:MaxPermSize=512m
   ```

## Production Deployment

### Google Play Store
1. Create signed AAB bundle: `./gradlew bundleRelease`
2. Upload `app-release.aab` to Google Play Console
3. Complete store listing and submit for review

### Direct Distribution
1. Generate signed APK: `./gradlew assembleRelease`
2. Enable "Unknown Sources" on target devices
3. Distribute APK file via email, website, or file sharing

### F-Droid (Open Source)
1. Ensure all dependencies comply with F-Droid guidelines
2. Follow F-Droid packaging instructions
3. Submit to F-Droid repository

## Next Steps for Production

Consider implementing:
- **Persistent Storage**: Room/SQLite database
- **Cloud Sync**: Firebase/Google Drive integration
- **User Authentication**: Google Sign-In/Firebase Auth
- **Material You**: Dynamic color support for Android 12+
- **Dark Mode**: Full dark theme implementation
- **Backup/Restore**: Note export/import functionality
- **Search**: Note search capabilities
- **Categories/Tags**: Note organization features