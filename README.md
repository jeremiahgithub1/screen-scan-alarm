# ScreenScanAlarm - Starter Project

This starter Android project contains a basic Kotlin app that:
- Requests screen capture permission (MediaProjection)
- Starts a foreground service that captures the screen and runs ML Kit on-device text recognition
- Allows selecting an MP3 from device storage as alarm
- Saves target text and triggers an alarm when the text is detected
- Plays alarm for up to 2 minutes

IMPORTANT:
- The zip includes source files and Gradle configuration, but **does not include the Gradle wrapper jar** (gradle/wrapper/gradle-wrapper.jar) or the executable gradlew binary.
  You can create the Gradle wrapper locally (using Android Studio or `gradle wrapper`) or I can add the wrapper files in a follow-up.
- Compile SDK is set to 33. If your workflow uses a different SDK, update the workflow variables.

Next steps:
1. Upload the contents of this project to your GitHub repo (do not include local.properties).
2. Add the GitHub Actions workflow `.github/workflows/build-apk.yml` (I provided this earlier).
3. Push to `main` and download the `app-debug.apk` artifact from Actions → run → Artifacts.

If you want, I can:
- Add the Gradle wrapper files into the zip (recommended), or
- Add extra features: rectangle selection UI, OCR tuning, or optimizations.
