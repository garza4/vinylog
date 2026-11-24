# VinyLog

VinyLog is an Android app for a physical media collection. Scan a barcode or type an entry, and it stays in an on-device library.

The home screen is **Select Library**. Vinyl, CDs, and Movies each open their own shelf. Video Games opens a list of systems, and each system has its own shelf. The system names are in `app/src/main/java/com/example/vinylog/games/GameSystems.kt`.

A shelf row shows the cover, title, artist, and year. Tap a row for the label, genre, format, barcode, and track list. Covers load from the URL saved with the entry.

## Catalog lookup

For vinyl, CDs, and movies, a barcode is looked up in this order:

1. MusicBrainz, by barcode
2. UPC Item DB, when the barcode is missing or the match is thin
3. MusicBrainz again, by album and artist
4. iTunes, for any tracks, year, or cover still missing

Shelves that already have a barcode but no tracks, year, or cover are filled in when that shelf opens. Video game barcodes are saved as entered and are not sent through this lookup.

Entries are stored in a Room database named `library`.

## Build

The app module is `:app`, package `com.example.vinylog`. It needs JDK 17 and an Android SDK. `minSdk` is 24 and `targetSdk` is 36.

From the project root:

```bash
bash gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

With a device or emulator connected:

```bash
bash gradlew :app:installDebug
adb shell am start -n com.example.vinylog/.MainActivity
```

Open this folder in Android Studio and let Gradle sync. The run configuration is the `app` module.

## Tests

```bash
bash gradlew :app:testDebugUnitTest
```
