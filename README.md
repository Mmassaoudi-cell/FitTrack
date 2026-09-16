# FitTrack

An offline Android app for workout tracking, nutrition logging and body-weight progress. No account or paid API is required.

## Version 1.1

### Workouts

- Start or resume an unfinished workout. Logged sets live in Room; the current exercise, draft weight/reps and rest deadline are saved separately for recovery.
- Search exercises by name or muscle group. Create routines with an ordered exercise list, or repeat the exercise order from your last completed workout.
- Enter an exact weight or use quick adjustments. View previous-session performance and copy a completed set.
- Complete sets from a persistent bottom action. Save operations prevent duplicate taps and allocate set numbers in a database transaction.
- Edit/delete sets during a workout and undo the most recent deletion. Finish saves before returning to the workout hub.
- Choose a rest duration, pause/resume, and toggle automatic rest. Timing recovers after interruptions; there is no background alarm or notification.

### Nutrition

- Navigate dates, search the local food library, favorite frequently used foods, and save reusable custom foods.
- Type an exact portion. Custom nutrition is entered **per 100 g**; calories and macros scale to the portion.
- Review daily calories, protein, carbohydrate and fat. Edit a logged portion or meal, delete entries, and undo the most recent deletion.
- Food photos use the bundled ML Kit image-labeling model on device. The app suggests a food only when a supported label meets the matching threshold. Broad labels such as “dessert” do not imply a specific dish.
- Review/correct the food and portion before saving. Unknown photos require food selection or manual nutrition entry; no default calorie guess is saved.
- Photo entries preserve the selected food's nutrition. Recognition confidence describes the label match, not calorie accuracy. The model does not measure portion mass or distinguish every recipe.
- Camera errors and denied permissions offer manual entry. Temporary captured photos are removed after analysis, with leftovers cleaned at next startup.

### Progress and settings

- Home provides direct entry points for workouts, food and body weight, plus optional profile setup guidance.
- Weight units apply to workout logging, body-weight input, history and charts.
- Progress charts show date ranges, numerical summaries and accessible descriptions. Points are spaced by time; training charts aggregate by session. Choose 30 days, 90 days or all time.
- Body weight supports dated entries and deletion/undo. Multiple entries on the same date are retained; the last recorded entry on that date is current.
- Daily queries refresh across midnight and time-zone changes. Estimated workout duration sums sessions independently, avoiding gaps between separate workouts.
- Calorie targets use the existing profile formula or a user override. Targets and calorie-burn values remain estimates, not medical advice. Historical nutrition screens compare against the current profile target.

## Screenshots

Screenshots from the Android 15 emulator are in [`docs/screenshots`](docs/screenshots). Device camera quality and food-recognition accuracy require testing with real food photos.

## Backup and privacy

Use **Settings → Export backup** to share/save a JSON copy outside the app. Version 2 backups include exercises, workouts, sets, routines, food items and favorites, food logs, body weights, profile and settings. Older version 1 backups remain supported; missing settings preserve current preferences.

Restoring a backup:

1. Reads and validates the selected file (maximum 25 MB), version, values and record references.
2. Shows the backup date and record counts and asks before replacing existing records.
3. Creates a recovery export of the current data before making changes.
4. Replaces database records in a transaction and restores settings, reporting errors or success.
5. Offers to export the recovery copy before returning to Home.

Database and preference storage are separate Android stores. Ordinary failures are rolled back; an abrupt device shutdown during the final cross-store update is not an atomic operation. The recovery export can be used to restore the previous state.

Automatic Android cloud backup and device transfer are explicitly excluded in the manifest and backup rules. Export a copy before uninstalling or changing phones. Exported JSON files are unencrypted; use a destination you trust. There is no app analytics or account service. ML Kit dependencies may contribute network permissions to the merged manifest; FitTrack's food recognition uses the bundled model and its own code makes no network requests.

## Architecture

Kotlin, Jetpack Compose / Material 3, Navigation Compose, Room, DataStore, coroutines, CameraX, bundled ML Kit and kotlinx.serialization. A small manual dependency container supplies repositories and preferences. ViewModels manage workout, food-entry and backup operations and expose observable state to Compose.

Database schema version 2 includes a tested migration from version 1. Existing workouts and food records are preserved when updating the app.

- `data/db`: entities, queries and migrations.
- `data/repository`: persistence operations and validation boundaries.
- `data/export`: versioned backup, validation, restoration and recovery.
- `data/prefs`: settings and active workout recovery state.
- `domain`: validation, unit conversion, day clock and calculations.
- `ui`: screens, state holders and shared controls.

## Build

Requirements: JDK 17, Android SDK 35, and access to Google/Maven Central for uncached dependencies. Minimum Android version: 8.0 (API 26).

```sh
./gradlew assembleDebug
./gradlew testDebugUnitTest lintDebug
./gradlew connectedDebugAndroidTest  # running emulator/device required
```

On Windows use `gradlew.bat`. Configure `JAVA_HOME` and `sdk.dir` in your local, untracked `local.properties`, or open the project in Android Studio.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (package `com.personal.fittrack.debug`). This is a test build, not a signed production release.

If this Windows machine's JDK reports `Unable to establish loopback connection` with `UnixDomainSockets`, using a short existing directory for `-Djdk.net.unixdomain.tmpdir` resolved the local build issue. This is an environment setting, not an app requirement.

## Verification

- Unit tests cover calculations, separated workout sessions, unit conversion, invalid numerical inputs, recognition matching, legacy/new backup serialization, invalid backup references and day/time-zone rollover.
- Android instrumentation tests cover concurrent set numbering, resume/finish, undo after a new set, migration from the exported v1 schema, backup round trips, rejection without mutation and database rollback after a settings failure.
- GitHub Actions runs build, unit tests and lint, plus a separate Android 35 emulator test job.
- See [`docs/VALIDATION.md`](docs/VALIDATION.md) for the actual local validation results and remaining device checks.

## Scope

Routines contain exercise order, not prescribed set targets. Nutrition comes from a small local starter library and user-entered foods; favorites and custom foods remain offline. Barcode lookup, cross-device sync, background timer alerts and a specialized food-recognition model are not included.

## License

See [LICENSE](LICENSE). Third-party libraries retain their own licenses.
