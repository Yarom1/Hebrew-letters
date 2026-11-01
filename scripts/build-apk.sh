#!/usr/bin/env bash
#
# Builds a signed, installable APK *without* Gradle or the Android SDK manager, using the
# Android build tools that Debian/Ubuntu package from AOSP:
#
#   sudo apt-get install aapt dalvik-exchange zipalign apksigner android-sdk-platform-23
#
# Usage:  scripts/build-apk.sh [output.apk]
#
# Signing: uses keystore/hebrew-letters.jks with the passwords in keystore/keystore.properties.
# If they do not exist, a new key is created there. The keystore/ folder is git-ignored on
# purpose: never publish the private key. Keep a copy: future versions of the app must be signed
# with the same key, or Android will refuse to install them over this one.
#
# (Android Studio / Gradle users can simply run ./gradlew assembleRelease instead.)

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP="$ROOT/app/src/main"
BUILD="$ROOT/build/manual"
OUT="${1:-$ROOT/apk/hebrew-letters.apk}"

SDK="${DEBIAN_ANDROID_SDK:-/usr/lib/android-sdk}"
ANDROID_JAR="${ANDROID_JAR:-$SDK/platforms/android-23/android.jar}"
DX="${DX:-$SDK/build-tools/debian/dx}"

KEY_DIR="$ROOT/keystore"
KEY_PROPS="$KEY_DIR/keystore.properties"

# Keep these in sync with app/build.gradle.kts.
gradle_value() { sed -n "s/^ *$1 *= *\"\{0,1\}\([^\"]*\)\"\{0,1\} *$/\1/p" "$ROOT/app/build.gradle.kts" | head -1; }
PACKAGE="$(gradle_value applicationId)"
VERSION_CODE="$(gradle_value versionCode)"
VERSION_NAME="$(gradle_value versionName)"
MIN_SDK="$(gradle_value minSdk)"
TARGET_SDK="$(gradle_value targetSdk)"

for tool in aapt2 javac zipalign apksigner keytool zip; do
  command -v "$tool" >/dev/null || { echo "Missing tool: $tool" >&2; exit 1; }
done
[ -f "$ANDROID_JAR" ] || { echo "Missing $ANDROID_JAR (apt-get install android-sdk-platform-23)" >&2; exit 1; }
[ -x "$DX" ] || { echo "Missing $DX (apt-get install dalvik-exchange)" >&2; exit 1; }

echo "Building $PACKAGE $VERSION_NAME ($VERSION_CODE), minSdk $MIN_SDK, targetSdk $TARGET_SDK"
rm -rf "$BUILD"
mkdir -p "$BUILD/gen" "$BUILD/classes" "$(dirname "$OUT")"

# The source manifest has no package attribute (Gradle supplies it); add it for aapt2.
sed "s#<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">#<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" package=\"$PACKAGE\">#" \
  "$APP/AndroidManifest.xml" > "$BUILD/AndroidManifest.xml"
grep -q "package=\"$PACKAGE\"" "$BUILD/AndroidManifest.xml" || { echo "Could not set the package name" >&2; exit 1; }

echo "1/6 resources"
aapt2 compile --dir "$APP/res" -o "$BUILD/res.zip"
aapt2 link -o "$BUILD/resources.apk" \
  -I "$ANDROID_JAR" \
  --manifest "$BUILD/AndroidManifest.xml" \
  -A "$APP/assets" \
  --java "$BUILD/gen" \
  --min-sdk-version "$MIN_SDK" --target-sdk-version "$TARGET_SDK" \
  --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" \
  "$BUILD/res.zip"

echo "2/6 java"
find "$APP/java" "$BUILD/gen" -name '*.java' > "$BUILD/sources.txt"
javac -nowarn -Xlint:-options -source 8 -target 8 -encoding UTF-8 \
  -bootclasspath "$ANDROID_JAR" -d "$BUILD/classes" @"$BUILD/sources.txt"

echo "3/6 dex"
"$DX" --dex --min-sdk-version="$MIN_SDK" --output="$BUILD/classes.dex" "$BUILD/classes"

echo "4/6 package"
cp "$BUILD/resources.apk" "$BUILD/unsigned.apk"
(cd "$BUILD" && zip -q -X unsigned.apk classes.dex)

echo "5/6 align"
zipalign -f -p 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"

echo "6/6 sign"
if [ ! -f "$KEY_PROPS" ]; then
  echo "No signing key found: creating a new one in $KEY_DIR (keep a private copy!)"
  mkdir -p "$KEY_DIR"
  PASS="$(head -c 18 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 20)"
  keytool -genkeypair -noprompt -keystore "$KEY_DIR/hebrew-letters.jks" -storetype PKCS12 \
    -storepass "$PASS" -keypass "$PASS" -alias hebrew-letters \
    -keyalg RSA -keysize 3072 -validity 18250 \
    -dname "CN=Hebrew Letters, O=Family" 2>/dev/null
  printf 'storeFile=hebrew-letters.jks\nstorePassword=%s\nkeyAlias=hebrew-letters\nkeyPassword=%s\n' \
    "$PASS" "$PASS" > "$KEY_PROPS"
fi
prop() { sed -n "s/^$1=//p" "$KEY_PROPS"; }
apksigner sign \
  --ks "$KEY_DIR/$(prop storeFile)" --ks-key-alias "$(prop keyAlias)" \
  --ks-pass "pass:$(prop storePassword)" --key-pass "pass:$(prop keyPassword)" \
  --min-sdk-version "$MIN_SDK" \
  --out "$OUT" "$BUILD/aligned.apk"
rm -f "$OUT.idsig"
apksigner verify --min-sdk-version "$MIN_SDK" "$OUT"

echo "Done: $OUT ($(du -h "$OUT" | cut -f1))"
