# SimpleNotes Android App

A simple Android notes application with basic CRUD functionality.

## Features

- **Notes List Activity**: Displays all notes in a RecyclerView with a Floating Action Button (FAB) to add new notes
- **Note Activity**: Create and edit notes with title and content fields
- **Material Design**: Uses Material Design components for modern UI
- **Responsive Layout**: Works on various screen sizes

## Project Structure

```
SimpleNotes/
├── app/
│   ├── build.gradle                 # App-level build configuration
│   ├── proguard-rules.pro           # ProGuard rules
│   └── src/main/
│       ├── AndroidManifest.xml      # App manifest
│       ├── java/com/simplenotes/    # Java source files
│       │   ├── Note.java            # Note data model
│       │   ├── NotesAdapter.java    # RecyclerView adapter
│       │   ├── NotesListActivity.java # Main activity
│       │   └── NoteActivity.java    # Note editor activity
│       └── res/                     # Resources
│           ├── layout/              # Layout XML files
│           │   ├── activity_notes_list.xml
│           │   ├── activity_note.xml
│           │   └── item_note.xml
│           ├── values/              # String and theme resources
│           └── drawable/            # Drawable resources
├── build.gradle                     # Project-level build configuration
├── gradle.properties               # Gradle configuration
└── settings.gradle                # Project settings
```

## Requirements

- Android Studio Arctic Fox (2020.3.1) or later
- Android SDK API 21 (Android 5.0) or later
- Gradle 8.0+
- Android Gradle Plugin 8.1.0+

## Building the App

### Method 1: Using Android Studio

1. Open Android Studio
2. Select "Open an existing Android Studio project"
3. Navigate to the `SimpleNotes` folder and select it
4. Wait for Gradle sync to complete
5. Click the "Run" button or use Build menu to build the project

### Method 2: Using Command Line

#### Debug APK
```bash
cd SimpleNotes
./gradlew assembleDebug
```

#### Release APK
```bash
cd SimpleNotes
./gradlew assembleRelease
```

#### Android App Bundle (AAB) for Play Store
```bash
cd SimpleNotes
./gradlew bundleRelease
```

## APK/AAB Location

After successful build, the files will be located in:
- **Debug APK**: `SimpleNotes/app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `SimpleNotes/app/build/outputs/apk/release/app-release.apk`
- **AAB**: `SimpleNotes/app/build/outputs/bundle/release/app-release.aab`

## Installation

### Debug APK
Enable "Unknown sources" in Android settings and install the APK file directly.

### Release APK
1. Sign the APK (if not already signed)
2. Enable "Unknown sources"
3. Install the APK

### Play Store (AAB)
Upload the AAB file to Google Play Console.

## Testing

The app can be tested on:
- Android Studio emulator
- Physical Android device
- Various Android API levels (21+)

## Development Notes

- The app currently stores notes in memory (no persistent storage)
- For production use, consider implementing a database (Room/SQLite)
- The UI uses Material Design components for consistency
- FAB provides easy access to add new notes
- RecyclerView efficiently handles note list display

## Deployment

### Google Play Store
1. Create a signed AAB bundle
2. Upload to Google Play Console
3. Configure store listing
4. Submit for review

### Direct Distribution
1. Build a signed release APK
2. Enable installation from unknown sources
3. Distribute APK file directly

## License

This is a sample project for educational purposes.