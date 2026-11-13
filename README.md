# Residency Counter (Android app + home-screen widget)

Counts the days spent in Saudi Arabia towards the **900 days within 5 years** needed (after the Premium Residency
date) to apply for Permanent Residency, and shows on a widget:

- days left and days done (of 900),
- whether the holder is inside Saudi Arabia (counter running) or abroad (counter paused),
- the expected application date, Gregorian and Hijri (Umm al-Qura).

Tap the widget to open the app (settings): Premium Residency date, **Record exit / Record return**, and the travel
record (add, edit, delete trips). Arabic and English: the phone's language until the button at the top of the app
switches it (the widget follows the app). The dates never leave the phone; the only network use is
**Check for update**.

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
| `app/src/personal/java/.../Defaults.java` | The dates the app starts with on first install (owner's record). Private data: keep this repository private. |
| `app/src/pub/java/.../Defaults.java` | Empty: the public update file carries no dates. |
| `Store.java` | Saved dates (SharedPreferences, JSON). The first run saves the starting dates at once. |
| `Summary.java` | Texts shown on the widget and the app card; Gregorian / Hijri formatting. |
| `CounterWidget.java` | The widget (refresh after midnight, hourly, on resize, after an update and on every edit). |
| `MainActivity.java` | The settings screen, language button, update check. |
| `Lang.java` | Interface language (phone's, or Arabic / English chosen in the app). |
| `Updater.java` | In-app update: `version.json` + APK from the public folder, checksum, Android's installer. |
| `res/layout/widget.xml` | The card, used by the widget and included at the top of the app. |
| `res/values/strings.xml`, `res/values-ar/strings.xml` | All texts, same keys in both. |
| `tools/publish-update.sh` | Builds the public APK and writes it with `version.json` into the site repository. |

No libraries beyond the Android framework (tests use JUnit + Robolectric).

## Two builds, one app

| Build | Dates inside | Where it goes |
| --- | --- | --- |
| `personal` | The owner's Premium Residency date and travel record | **First install**: Releases of this private repository (built by GitHub Actions on every push to `main`). |
| `pub` | None | **In-app update**: `https://dromaia.github.io/residency/` (`version.json` + `residency-counter.apk`), public. |

Both have the same application id and are signed with `app/app.keystore`, so the update installs over the first
install and keeps the dates saved on the phone. Never put dates in `main/` or `pub/` code.

## Build, test, publish an update

```bash
# needs the Android SDK (platform 35, build-tools 35)
./gradlew testPersonalDebugUnitTest testPubDebugUnitTest assemblePersonalRelease assemblePubRelease
# APKs: app/build/outputs/apk/{personal,pub}/release/ ; screen pictures from the tests: app/build/screens/
```

For every change:
1. Bump `versionCode` (and `versionName`) in `app/build.gradle`; add or update a test; run the tests.
2. Commit and push to `main` (GitHub Actions publishes the `personal` APK under Releases).
3. `tools/publish-update.sh /path/to/DrOmaia.github.io`, then commit and push that repository to `main`.
   The app checks `version.json` when it opens (at most every 6 hours) and from **Check for update**.
