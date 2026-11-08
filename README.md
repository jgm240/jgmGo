# jgmGo

> Unofficial hobby project. It is not affiliated with, endorsed by or sponsored by Nokia, HMD Global, Qualcomm or Google.
> "Nokia 8110 4G" is used only to say which phone this targets. This is a build of the Android Open Source Project (AOSP),
> version 8.1 Go; it is not an official Android product and does not use the Android logo or wordmark.

An experiment to run an AOSP 8.1 Go build on the **Nokia 8110 4G** ("banana phone", Snapdragon 205, ~400 MB usable RAM).

**Status: early.** Nothing here is a finished ROM, and no system image is published.

What has been shown on a real phone:
- the bootloader accepts an unsigned custom boot image;
- a bare kernel + a tiny init drives the 240x320 screen through the plain framebuffer and reads the keypad (`experiments/fbtest`).

What is in this repo:
- `jgmKeys/`: use the whole phone with the keypad. Arrow-key navigation, soft keys with a label bar, number hints (long-press #), a free pointer that snaps to buttons (long-press *), type-ahead in lists, spoken navigation, slider handling and a T9 keyboard (# cycles Abc / abc / ABC / 123 / predictive).
- `jgmLauncher/`: a home screen where the number keys are the launcher (2-9 apps, 1 voicemail, 0 all apps, hold 0 flashlight).
- `jgmVoice/`: offline voice assistant (hold the Call key): call, text, open apps, time, timers, alarms, flashlight, Wi-Fi, Bluetooth, volume.
- `jgmTweaks/`: keypad-first app to change zram size, swappiness and an optional swap file.
- `device/jgmgo/`: product makefile, key layouts, init script and SELinux file; `tools/stage-device.sh` copies everything into an AOSP tree.
- `branding/`: the boot animation (`bootanimation-jgmgo.zip`) and the script that draws it.
- `experiments/fbtest/`: the display and keypad test.

**Tested so far:** the logic of every app (navigation, typing, hints, pointer, voice commands, launcher shortcuts) passes automated checks, and all the Android code compiles against the Android 8.1 SDK. None of it has run on a phone yet, because there is no jgmGo build yet.

Not here, on purpose: any Nokia/Qualcomm firmware or vendor binaries, or AOSP source. See `NOTICE.md`.

Licence: Apache-2.0. See `LICENSE` and `NOTICE.md`.
