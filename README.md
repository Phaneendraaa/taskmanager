# Task Manager (Kotlin + Jetpack Compose)

Simple offline task manager. Frontend only: add, complete, delete, filter
(All / Active / Done), clear done. Tasks are saved on the device.

## Build the APK (no Android Studio needed)

Requirements: JDK 17+ and the Android SDK command-line tools.

1. Install the SDK packages (once):
   ```
   sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
   ```
2. Tell Gradle where the SDK is, in `local.properties`:
   ```
   sdk.dir=/path/to/android-sdk
   ```
3. Build:
   ```
   ./gradlew assembleDebug
   ```
4. The APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Or with Android Studio

Open this folder, let Gradle sync, press Run.

## Install on your phone

Copy `app-debug.apk` to the phone and open it (allow "Install unknown apps"),
or run `adb install -r app-debug.apk`.
