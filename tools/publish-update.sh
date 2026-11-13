#!/bin/bash
# Publishes the current version as the in-app update.
# Builds the "pub" APK (no dates inside) and writes it with version.json into the public site repository:
#   tools/publish-update.sh /path/to/DrOmaia.github.io
# Then commit and push that repository to main (GitHub Pages); phones see the update at the next check.
set -e
site="${1:?usage: tools/publish-update.sh /path/to/DrOmaia.github.io}"
cd "$(dirname "$0")/.."
./gradlew -q --console=plain testPubDebugUnitTest assemblePubRelease
apk=app/build/outputs/apk/pub/release/app-pub-release.apk
code=$(grep -oP 'versionCode \K[0-9]+' app/build.gradle)
name=$(grep -oP "versionName '\K[^']+" app/build.gradle)
sha=$(sha256sum "$apk" | cut -d' ' -f1)
mkdir -p "$site/residency"
cp "$apk" "$site/residency/residency-counter.apk"
printf '{"versionCode": %s, "versionName": "%s", "sha256": "%s"}\n' "$code" "$name" "$sha" > "$site/residency/version.json"
echo "published $name ($code) to $site/residency"
