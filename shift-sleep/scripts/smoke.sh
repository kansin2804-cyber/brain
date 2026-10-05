#!/usr/bin/env bash
# Device/emulator smoke for ShiftSleep (run on a machine with adb + device).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

echo "== unit tests =="
./gradlew :plan_engine:test :app:testDebugUnitTest --quiet

DEVICE=$(adb devices | awk '/\tdevice$/{print $1; exit}')
if [[ -z "${DEVICE:-}" ]]; then
  echo "No adb device online — unit tests passed. Connect a phone/emulator for UI smoke."
  exit 0
fi

echo "== install debug APK on $DEVICE =="
./gradlew :app:installDebug --quiet

echo "== launch + dump UI hierarchy snippets =="
adb -s "$DEVICE" shell am force-stop com.shiftsleep.app || true
adb -s "$DEVICE" shell pm clear com.shiftsleep.app || true
adb -s "$DEVICE" shell am start -n com.shiftsleep.app/.MainActivity
sleep 3
adb -s "$DEVICE" shell uiautomator dump /sdcard/shiftsleep-ui.xml
adb -s "$DEVICE" shell cat /sdcard/shiftsleep-ui.xml | tr '>' '>\n' | \
  grep -E '교대수면|시작하기|병원|오늘|근무표|설정|면책|취침|기상' | head -40

echo "Smoke dump complete. Manually tap through: 시작하기 → 홈 플랜 → 근무표 변경 → 설정 알림."
