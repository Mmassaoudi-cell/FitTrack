# Validation — FitTrack 1.1

Local validation completed September 16, 2026, using Java 17, Android SDK 35 and the existing Android 15 x86_64 emulator.

## Automated checks

| Check | Result |
| --- | --- |
| Debug APK build | Passed |
| Unit tests | 32 passed, 0 failures |
| Android persistence tests | 8 passed, 0 failures |
| Android lint | 0 errors; 62 warnings, mostly dependency/toolchain update notices |
| Whitespace/diff check | Passed |

The Android tests exercised migration from the exported v1 database schema, concurrent set insertion, start/resume/finish, deletion/undo numbering, backup round trips (including settings, favorites and routines), invalid-import rejection, and rollback after a simulated settings-write failure.

The final UI-only changes were followed by another successful APK build, unit-test run and lint check. The database test suite passed on the same final persistence implementation.

## Emulator interaction checks

- Started a workout, selected an exercise, logged a set, force-closed the app and resumed with the exercise and completed set preserved.
- Finished a workout and returned successfully, including a session started directly from Home.
- Logged a food, changed its portion from 100 g to 200 g, and verified calories doubled from 52 to 104 kcal.
- Deleted and restored the food entry using Undo; checked previous-day and next-day navigation.
- Opened food-photo capture without permission and successfully reached manual food entry.
- Logged body weight and checked the resulting progress screen.
- Switched between light and dark themes and inspected the screens.
- Created a named routine, selected an exercise, saved it, and started a workout from it.
- Confirmed the workout save action remained available at 150% text size and after landscape rotation. Restored emulator font and rotation settings afterward.

Screenshots are in [screenshots](screenshots). They contain sample emulator records.

## Artifact

- Local installable artifact: `build/outputs/FitTrack-1.1-debug.apk`.
- Version: 1.1 (version code 2).
- Application ID: `com.personal.fittrack.debug`.
- SHA-256: `539b67b6c24073d57ff07cf3cbf2b18c3a28f1697cf73cf3f1723d758d206590`.

This is a debug build for testing. Production signing and store publication have not been performed. GitHub Actions is configured to run build, unit-test, lint and emulator checks on pushes and pull requests. The results above describe the local validation run.

## Remaining checks

- Physical-device camera quality, recognition accuracy and behavior with real food photos.
- Broader Android-version and device coverage beyond the Android 15 emulator.
- The timer has no background alarm/notification; it recovers its deadline when the app is reopened.
- Database and settings use separate storage systems. Ordinary restore failures roll back, but a device shutdown between their final commits is not atomic; the pre-restore recovery export is retained.
