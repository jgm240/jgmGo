# a8.1_81104g

"a8.1_81104g" = Android 8.1 for the Nokia 8110 4G. An experiment to run **Android 8.1 Go** on the **Nokia 8110 4G** ("banana phone", Snapdragon 205, ~400 MB usable RAM).

> Unofficial hobby project. It is not affiliated with, endorsed by or sponsored by Nokia, HMD Global, Qualcomm or Google.
> "Nokia 8110 4G" and "Android" are used only to say which phone and which Android version this targets.

**Status: early.** Nothing here is a finished ROM, and no system image is published.

What has been shown on a real phone:
- the bootloader accepts an unsigned custom boot image;
- a bare kernel + a tiny init drives the 240x320 screen through the plain framebuffer and reads the keypad (`experiments/fbtest`).

What is in this repo:
- `jgmTweaks/`: keypad-first app to change zram size, swappiness and an optional swap file (system app + root helper + SELinux).
- `branding/`: the boot animation (`bootanimation-jgmdroid.zip`) and the script that draws it.
- `experiments/fbtest/`: the display and keypad test.

Not here, on purpose: any Nokia/Qualcomm firmware or vendor binaries, or Android source. See `NOTICE.md`.

Licence: Apache-2.0. See `LICENSE` and `NOTICE.md`.
