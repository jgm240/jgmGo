#!/bin/sh
# Copies the apps into an AOSP tree: tools/stage-device.sh /path/to/aosp
set -e
T="${1:?usage: stage-device.sh /path/to/aosp}"; R="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$T/device/jgmgo" "$T/packages/apps"
cp -R "$R/device/jgmgo/." "$T/device/jgmgo/"
cp -R "$R/jgmKeys/app" "$T/packages/apps/JgmKeys"; cp -R "$R/jgmTweaks/app" "$T/packages/apps/JgmTweaks"
cp -R "$R/jgmLauncher/app" "$T/packages/apps/JgmLauncher"; cp -R "$R/jgmVoice/app" "$T/packages/apps/JgmVoice"
echo "staged into $T"
