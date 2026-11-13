# Residency Counter (Android app + home-screen widget)

Counts the days spent in Saudi Arabia towards the **900 days within 5 years** needed (after the Premium Residency
date) to apply for Permanent Residency, and shows on a widget:

- days left and days done (of 900),
- whether the holder is inside Saudi Arabia (counter running) or abroad (counter paused),
- the expected application date, Gregorian and Hijri (Umm al-Qura).

Tap the widget to open the app (settings): Premium Residency date, **Record exit / Record return**, and the travel
record (add, edit, delete trips). Arabic and English, by the phone's language. Nothing leaves the phone.

## Counting rule (`Calc.java`)

- A day counts from the Premium Residency date (that day included) when the whole day is inside Saudi Arabia:
  **the exit day and the return day are not counted**.
- A trip with no return date freezes the counter; the expected date assumes a return today, so it moves one day
  later every day until the return is recorded.
- Expected date = the day the 900th day is completed. A warning shows in the app if it falls after the 5 years.
- "Today" is always Riyadh time.

## Files

| File | What it holds |
| --- | --- |
| `app/src/main/java/.../Calc.java` | The rule (no Android code; unit tested). |
| `Defaults.java` | The dates the app starts with on first install (owner's travel record). Private data: keep this repository private. |
| `Store.java` | Saved dates (SharedPreferences, JSON). |
| `Summary.java` | Texts shown on the widget and the app card; Gregorian / Hijri formatting. |
| `CounterWidget.java` | The widget (refresh after midnight, hourly, on resize and on every edit). |
| `MainActivity.java` | The settings screen. |
| `res/layout/widget.xml` | The card, used by the widget and included at the top of the app. |
| `res/values/strings.xml`, `res/values-ar/strings.xml` | All texts, same keys in both. |

No libraries beyond the Android framework (tests use JUnit + Robolectric).

## Build, test, install

```bash
./gradlew testDebugUnitTest assembleRelease   # needs the Android SDK (platform 35, build-tools 35)
# APK: app/build/outputs/apk/release/app-release.apk ; screen pictures from the tests: app/build/screens/
```

Every push to `main` builds the APK on GitHub Actions and publishes it under **Releases**
(`residency-counter.apk`). All builds are signed with `app/app.keystore`, so a new version installs over the old
one and keeps the saved dates. Bump `versionCode` in `app/build.gradle` on every change.
