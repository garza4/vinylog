# VinyLog

VinyLog is an Android app for a physical media collection. Scan a barcode or type an entry, and it stays in an on-device library.

The home screen is **Select Library**. Vinyl, CDs, Movies, Books, and TV Shows each open their own shelf. Video Games opens a list of systems, and each system has its own shelf. The system names are in `app/src/main/java/com/example/vinylog/games/GameSystems.kt`.

A shelf row shows the cover, title, and the main credit and year. The fields you enter depend on the library. Vinyl and CDs use artist, label, and tracks. Movies use director, studio, cast, runtime, and format. Books use author, publisher, pages, format, and ISBN. Games use developer, publisher, rating, and players. TV shows use creator, network, seasons, episodes, cast, and format. Release year, genre, barcode, and cover are shared. Covers load from the URL saved with the entry.

## Catalog lookup

Scanning a barcode lists the catalog matches and waits for you to pick one. Nothing is saved until you choose a match, or enter the details yourself. Vinyl and CDs list each MusicBrainz pressing, and iTunes when the barcode is not in MusicBrainz. Movies, TV shows, and games list Wikipedia matches for the product title. Books list Open Library editions. The match you pick fills that shelf's fields.

Vinyl and CD shelves that already have a barcode but no tracks, year, or cover are filled in when that shelf opens.

Open an entry to see its details. The pencil at the top right edits the saved fields. The trash can asks you to confirm, then removes that entry from the shelf. Search finds other catalog matches, and the one you pick replaces the entry's details, including the fields for that library. Vinyl and CDs search MusicBrainz and iTunes. Movies, TV shows, and games search Wikipedia and Wikidata. Books search Open Library.

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
