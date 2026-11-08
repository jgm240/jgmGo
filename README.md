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
- `jgmTweaks/`: keypad-first app to change zram size, swappiness and an optional swap file (system app + root helper + SELinux).
- `jgmKeys/`: use the whole phone with the keypad: arrow-key navigation, soft keys, a free pointer and a T9 keyboard (no touchscreen needed).
- `branding/`: the boot animation (`bootanimation-jgmgo.zip`) and the script that draws it.
- `experiments/fbtest/`: the display and keypad test.

Not here, on purpose: any Nokia/Qualcomm firmware or vendor binaries, or AOSP source. See `NOTICE.md`.

Licence: Apache-2.0. See `LICENSE` and `NOTICE.md`.
