# FitTrack

A free, offline-first Android fitness app: workout tracking with fast rep/weight logging,
progressive-overload insight, workout history and progress charts, a calorie/nutrition
tracker with an on-device food-photo calorie estimator, body-weight tracking, and local
JSON backup/restore. No account, no login, no server, no paid APIs.

- Package: `com.personal.fittrack`
- Min SDK: 26 (Android 8.0) &nbsp;·&nbsp; Target/Compile SDK: 35

## Screenshots

Not included in this build (no emulator/device was available in the build environment —
see **Known limitations**). Install the APK to see the live UI.

## Architecture

MVVM with a simple hand-written dependency container (no Hilt/Dagger, to keep the build
lean and dependency-free of annotation-processor complexity):

```
com.personal.fittrack
├── FitTrackApp.kt            Application; owns AppContainer, seeds default data
├── MainActivity.kt           Hosts the Compose NavHost + theme
├── di/AppContainer.kt        Manual DI: Room DB, repositories, DataStore, export manager
├── data/
│   ├── db/                   Room entities, DAOs, AppDatabase
│   ├── repository/           WorkoutRepository, NutritionRepository, BodyWeightRepository
│   ├── prefs/                UserPreferences (Jetpack DataStore: units, targets, profile)
│   ├── seed/                 Default exercise list + local nutrition database
│   └── export/               JSON backup/restore (kotlinx.serialization)
├── domain/                   Pure calculation logic: volume, BMR/TDEE, unit conversion
└── ui/
    ├── home, workout, nutrition, progress, settings   (one package per bottom-nav tab)
    ├── components/            Reusable Compose widgets (RepCounter, WeightStepper, chart…)
    └── nutrition/vision/      ML Kit food-recognition wrapper + label→food mapping
```

State flows from Room/DataStore → Repository → ViewModel (`StateFlow`) → Compose screen.
All screens are single-Activity Jetpack Compose with Navigation-Compose and Material 3.

## Dependencies

Everything below is free and has no usage cap or account requirement:

| Purpose | Library |
|---|---|
| UI | Jetpack Compose, Material 3, Navigation-Compose |
| Persistence | Room (workouts, food log, body weight), DataStore Preferences (settings) |
| Camera | CameraX (core, camera2, lifecycle, view) |
| Food recognition | ML Kit Image Labeling (bundled model) |
| Backup | kotlinx.serialization (JSON) |
| Async | Kotlin Coroutines + Flow |
| Tests | JUnit4, kotlinx-coroutines-test |

Gradle 8.9, Android Gradle Plugin 8.5.2, Kotlin 1.9.24.

## AI model used for food recognition

**ML Kit Image Labeling — bundled base model** (`com.google.mlkit:image-labeling:17.0.9`).

- **Source / docs:** https://developers.google.com/ml-kit/vision/image-labeling
- **License:** Apache License 2.0 (Google)
- **Why this one:** it is a general-purpose, ~4 MB on-device classifier that ships inside
  the app (no download, no Play Services requirement, works with airplane mode on) and is
  officially free for any volume of use. It was chosen over training/bundling a custom
  Food-101 TensorFlow Lite model because that would require a training pipeline and a much
  larger model file for, at this scope, an accuracy gain that user-confirmed portion entry
  already compensates for. It was chosen over a cloud vision API because those require a
  paid key or per-call billing, which conflicts with the "must be free forever, must work
  offline" requirement.
- **How it's used:** `FoodRecognizer` (`ui/nutrition/vision/FoodRecognizer.kt`) runs the
  labeler on the captured photo and returns generic labels ("Pizza", "Fruit", "Baked
  goods", confidence 0–1). `FoodLabelMapper` maps recognized keywords to entries in the
  local nutrition database. The result is always shown to the user for confirmation and
  portion adjustment before it is saved — see the required workflow below.
- **Honest limitation:** this is a scene/object labeler, not a dish-specific model. It
  recognizes broad categories well (pizza, salad, fruit, bread, fries…) but will not
  distinguish, e.g., chicken curry from beef curry. When no confident match exists, the
  app asks the user to search/enter the food manually instead of guessing.

### Required food-photo workflow (implemented exactly as specified)

```
Photo → ML Kit labels → keyword match against local DB → user adjusts portion (g)
      → calories computed from per-100g data → confidence badge shown
      → user taps Edit / Confirm / Retake before anything is saved
```

Confidence is always shown as **High / Medium / Low**, derived from the model's own
confidence score, and every screen states these are *estimates*.

## Nutrition data source

A local, offline `food_items` Room table (`data/seed/DefaultFoods.kt`) seeded on first run
with ~30 common foods (chicken breast, rice, pasta, bread, eggs, milk, cheese, beef, fish,
salmon, tuna, potatoes, fries, banana, apple, orange, avocado, broccoli, salad, yogurt,
oatmeal, peanut butter, pizza, burger, sandwich, coffee, sushi, hot dog, ice cream…) with
calories/protein/carbs/fat per 100 g, approximated from typical **USDA FoodData Central**
values (https://fdc.nal.usda.gov/, public domain). Users can search this table, add their
own foods, or type a one-off manual entry — the app never needs a network connection to
log food.

## Calorie calculator

`domain/CalorieCalculator.kt` implements the **Mifflin-St Jeor** equation (a standard,
widely cited BMR estimate) combined with an activity multiplier for TDEE, then applies a
±500/+400 kcal adjustment for lose/maintain/gain goals. The Settings screen states plainly
that this is an estimate, not medical advice, and lets the user override the computed
target with any manual value.

## Privacy

- No account, no login, no analytics SDK, no ads SDK.
- Food photos are analyzed **entirely on-device**; they are never uploaded anywhere.
- All fitness/nutrition/body-weight data stays in the app's local Room database unless the
  user explicitly taps **Export Data** (writes a JSON file and opens the normal Android
  share sheet) or **Import Data** (reads a JSON file the user picks).
- The built APK declares `INTERNET` / `ACCESS_NETWORK_STATE` permissions purely because
  the ML Kit library depends on Google Play services' common code, which declares them
  defensively; FitTrack's own code makes no network calls.

## Known limitations

- Food recognition is a general labeler, not a dish-specific model — see above. Always
  confirm/adjust the detected food and portion before saving.
- Barcode scanning (spec section 17, explicitly optional) was left out to keep the build
  dependency-light; Settings/Add Food's manual search covers the same need today.
- Calorie-burn and calorie-target numbers are heuristic estimates (see in-app disclaimers),
  not medical advice.
- **This build was produced and unit-tested in a headless CI-style environment with no
  Android emulator or physical device attached**, so while `assembleDebug` and all unit
  tests pass, full on-device UI interaction (tapping through every screen) has not been
  manually exercised by the assistant. Please treat first-run testing on your phone as
  part of setup, and report anything that looks wrong.

## Build instructions

### Option A — Android Studio
1. Open this folder (`fitness app/`) as a project in a recent Android Studio (Ladybug or
   newer recommended).
2. Let Gradle sync (it will download dependencies from Google's and Maven Central's public
   repositories — no login required).
3. Run the `app` configuration on an emulator or a USB-connected phone, **or** use
   **Build → Build Bundle(s) / APK(s) → Build APK(s)** to produce the APK described below.

### Option B — command line
```
# from the project root, with a JDK 17 on PATH (or JAVA_HOME set)
./gradlew assembleDebug
```

## APK location

```
app/build/outputs/apk/debug/app-debug.apk
```

This debug build was produced and verified in this session:
`gradlew assembleDebug` → **BUILD SUCCESSFUL**, `gradlew testDebugUnitTest` → **all tests
passed** (volume math, BMR/TDEE math, kg↔lb conversion).

## Installing on your Android phone

### Method 1 — install the APK directly (no Android Studio needed)
1. Copy `app-debug.apk` to your phone (USB cable, email to yourself, cloud drive, etc.).
2. On your phone, open the file from Downloads/Files.
3. Android will prompt "Install unknown apps" the first time — allow it for the app you
   used to open the file (Files/Chrome/etc.).
4. Tap **Install**, then **Open**.

### Method 2 — run from Android Studio over USB
1. On your phone: **Settings → About phone → tap "Build number" 7 times** to unlock
   Developer Options.
2. **Settings → Developer options → enable USB debugging.**
3. Connect the phone via USB and accept the "Allow USB debugging?" prompt.
4. In Android Studio, select your device in the toolbar dropdown and press **Run ▶**.

## Testing performed in this session

1. ✅ Toolchain provisioned from scratch (JDK 17, Android SDK platform 35 + build-tools
   35.0.0 + platform-tools, Gradle 8.9) — none were pre-installed.
2. ✅ `gradlew assembleDebug` — clean, successful build, zero errors.
3. ✅ `gradlew testDebugUnitTest` — all JVM unit tests pass.
4. ✅ Compose navigation graph compiles with typed `Long` args for session/exercise ids.
5. ✅ Room compiles via KSP; schema exported to `app/schemas/…/1.json` (single version, no
   migration needed yet).
6. ✅ CameraX + runtime `CAMERA` permission flow compiles and follows the standard
   `ActivityResultContracts.RequestPermission` pattern.
7. ⚠️ ML Kit inference wiring compiles and follows Google's documented API; not exercised
   on a live camera frame in this environment (no device/emulator available — see *Known
   limitations*).
8. ✅ Calorie/BMR/TDEE math covered by `CalorieCalculatorTest` (unit tests, all passing).
9. ⚠️ Rep +1/-1/reset logic reviewed by hand (`ActiveWorkoutViewModel`); not covered by an
   automated test in this pass.
10. ⚠️ Weight increment/decrement logic reviewed by hand; underlying kg↔lb math is unit
    tested (`UnitConverterTest`).
11. ⚠️ Workout history persistence relies on Room, which is exercised indirectly through
    the successful schema export and compile; not manually clicked through on a device.
12. ✅ kg↔lb conversion covered by `UnitConverterTest` (round-trip + known values).
13. ✅ Debug APK built at the path above.

Items marked ⚠️ are implemented and code-reviewed but not click-tested on a real device in
this environment — please do a quick manual pass after installing.
