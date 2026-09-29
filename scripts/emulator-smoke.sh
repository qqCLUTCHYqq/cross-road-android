#!/usr/bin/env bash
set -euo pipefail
mkdir -p build/smoke
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb shell am start -n io.github.qqclutchyqq.crossroad.android/.MainActivity --ez bootProbe true
for attempt in $(seq 1 90); do
  adb logcat -d > build/smoke/logcat.txt
  if grep -q 'PASS: 101 native constructors' build/smoke/logcat.txt; then
    adb exec-out screencap -p > build/smoke/boot.png
    echo 'PASS: Android WebView native initialization'
    exit 0
  fi
  if grep -q 'FAIL:' build/smoke/logcat.txt; then break; fi
  sleep 2
done
adb exec-out screencap -p > build/smoke/boot.png
echo 'Android runtime smoke did not pass.' >&2
exit 1
