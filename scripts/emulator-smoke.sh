#!/usr/bin/env bash
set -euo pipefail
mkdir -p build/smoke
trap 'adb logcat -d > build/smoke/final-logcat.txt || true; grep -E "CrossRoad|CONSOLE|AndroidRuntime" build/smoke/final-logcat.txt | tail -n 100 || true; adb exec-out screencap -p > build/smoke/final.png || true' EXIT
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb shell am start -n io.github.qqclutchyqq.crossroad.android/.MainActivity --ez bootProbe true
for attempt in $(seq 1 90); do
  adb logcat -d > build/smoke/logcat.txt
  if grep -q 'PASS: 101 native constructors' build/smoke/logcat.txt; then
    adb exec-out screencap -p > build/smoke/boot.png
    echo 'PASS: Android WebView native initialization'
    break
  fi
  if grep -q 'FAIL:' build/smoke/logcat.txt; then break; fi
  sleep 2
done
grep -q 'PASS: 101 native constructors' build/smoke/logcat.txt
grep -q 'PASS: IndexedDB probe write committed' build/smoke/logcat.txt
adb shell am force-stop io.github.qqclutchyqq.crossroad.android
adb logcat -c
adb shell am start -n io.github.qqclutchyqq.crossroad.android/.MainActivity --ez bootProbe true
for attempt in $(seq 1 60); do
  adb logcat -d > build/smoke/reopen-logcat.txt
  if grep -q 'PASS: IndexedDB persisted across process restart' build/smoke/reopen-logcat.txt; then break; fi
  sleep 2
done
grep -q 'PASS: IndexedDB persisted across process restart' build/smoke/reopen-logcat.txt
adb shell am force-stop io.github.qqclutchyqq.crossroad.android
adb logcat -c
adb shell am start -n io.github.qqclutchyqq.crossroad.android/.MainActivity --ez gameSmoke true
for attempt in $(seq 1 120); do
  adb logcat -d > build/smoke/game-logcat.txt
  if grep -q 'PASS: first runtime render loop' build/smoke/game-logcat.txt; then
    sleep 20
    adb exec-out screencap -p > build/smoke/game.png
    echo 'PASS: remote Content and game render loop; screenshot needs visual review'
    exit 0
  fi
  if grep -q 'FAIL:' build/smoke/game-logcat.txt; then break; fi
  sleep 2
done
adb exec-out screencap -p > build/smoke/game.png
echo 'Android remote game startup did not pass.' >&2
exit 1
