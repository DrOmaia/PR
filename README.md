# Residency Counter (Android app + home-screen widget)

Counts the days spent in Saudi Arabia towards the **900 days within 5 years** needed (after the Premium Residency
date) to apply for Permanent Residency, and shows on a widget:

- days left and days done (of 900),
- whether the holder is inside Saudi Arabia (counter running) or abroad (counter paused),
- the expected application date, Gregorian and Hijri (Umm al-Qura).

Tap the widget to open the app (settings): Premium Residency date, **Record exit / Record return**, the travel
record (add, edit, delete trips), and **Widget appearance** (which items are shown — one switch for each of the
ten items; colour and opacity; size of the big number and of the other texts; three arrangements; swap sides; live
preview at the widget's real size). The widget can be resized down to one cell: what stays grows to fill the space,
and if the space is too small for the chosen items the least important ones are left out (`Look.dropOne`). Arabic and English: the phone's language until the button at the top of the app
switches it (the widget follows the app). The dates never leave the phone; the only network use is
**Check for update**.

## Stable version

**1.6** is the version the owner approved as stable on 13 Nov 2026: commit `402375e`, tag and release `build-8`
(its APK is attached to that release). Build new work on top of it; to see or return to it:
`git checkout build-8`. Earlier stable: 1.5 (`build-5`). When she approves a later version, update this line with
its `build-N` tag.

## Counting rule (`Calc.java`)

- A day counts from the Premium Residency date (that day included) when the whole day is inside Saudi Arabia:
  **the exit day and the return day are not counted**.
- A trip with no return date freezes the counter; the expected date assumes a return today, so it moves one day
  later every day until the return is recorded.
- Expected date = the day the 900th day is completed. A warning shows in the app if it falls after the
  residency's **expiry date**. The owner's is kept exactly as printed on her residency (`Defaults.end`: 2 April
  2030, shown there as 29 Dhu al-Qi'dah 1451); otherwise it is worked out as 5 Hijri years from the issue date
  minus one day (`Calc.defaultEnd`), and any date can be set by hand in the app. Official records can differ by a
  day from the phone's Hijri conversion, so a given Hijri date is shown as given, not converted.
- "Today" is always Riyadh time.

## Files

| File | What it holds |
| --- | --- |
| `app/src/main/java/.../Calc.java` | The rule (no Android code; unit tested). |
| `app/src/personal/java/.../Defaults.java` | The dates the app starts with on first install (owner's record). |
| `app/src/pub/java/.../Defaults.java` | Empty: a build with no dates inside. |
| `Store.java` | Saved dates (SharedPreferences, JSON). The first run saves the starting dates at once. |
| `Summary.java` | Texts shown on the widget and the app card; Gregorian / Hijri formatting. |
| `CounterWidget.java` | The widget (refresh after midnight, hourly, on resize, after an update and on every edit). `build()` measures the views off-screen and gives the big number the largest size at which every text still fits (`fits()`); crowded small widgets get smaller side texts. `preview()` draws the same widget inside the app. |
| `Look.java`, `LookActivity.java` | The widget's look (saved on the phone) and the "Widget appearance" screen. |
| `MainActivity.java` | The settings screen, language button, update check. |
| `Lang.java` | Interface language (phone's, or Arabic / English chosen in the app). |
| `Updater.java` | In-app update: `version.json` + APK from the public folder, checksum, Android's installer. |
| `DrOmaia.github.io/pr/index.html` (site repository) | The download page (Arabic / English); reads `version.json`, so it needs no edit on an update. |
| `res/layout/widget.xml`, `widget_center.xml`, `widget_dates_top.xml` | The three arrangements; same view ids in all (a tinted rounded image is the card, so any colour works). |
| `res/values/strings.xml`, `res/values-ar/strings.xml` | All texts, same keys in both. |
| `tools/publish-update.sh` | Builds the public APK and writes it with `version.json` into the site repository. |

No libraries beyond the Android framework (tests use JUnit + Robolectric).

## Two builds, one app

| Build | Dates inside | Where it goes |
| --- | --- | --- |
| `personal` | The owner's Premium Residency date and travel record | The download page <https://dromaia.github.io/pr/> and the in-app update (`https://dromaia.github.io/residency/`: `version.json` + `residency-counter.apk`); also Releases of this repository (GitHub Actions, every push to `main`). |
| `pub` | None | Not published; kept for a copy to share without the dates (`tools/publish-update.sh <site> pub`). |

The owner chose (13 Nov 2026) to have her own version, with her dates, as the file on the public page.
Both builds have the same application id and are signed with `app/app.keystore`, so any of them installs over the
other and keeps the dates saved on the phone. Dates entered on a phone are never replaced; a phone where nothing
was entered takes the starting dates of the `personal` build.

## Build, test, publish an update

```bash
# needs the Android SDK (platform 35, build-tools 35)
./gradlew testPersonalDebugUnitTest testPubDebugUnitTest assemblePersonalRelease assemblePubRelease
# APKs: app/build/outputs/apk/{personal,pub}/release/ ; screen pictures from the tests: app/build/screens/
```

For every change:
1. Bump `versionCode` (and `versionName`) in `app/build.gradle`; add or update a test; run the tests.
2. Commit and push to `main` (GitHub Actions also publishes the `personal` APK under Releases).
3. `tools/publish-update.sh /path/to/DrOmaia.github.io`, then commit and push that repository to `main`.
   The app checks `version.json` when it opens (at most every 6 hours) and from **Check for update**.
