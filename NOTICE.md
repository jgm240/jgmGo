# Notices

## What this repository contains
Only original work for the jgmDroid experiment: jgmTweaks (app + init script + SELinux file), the boot animation and
its generator, and a display/keypad test program. It is licensed under the Apache License 2.0 (see `LICENSE`).

## What it deliberately does not contain
- No Nokia / HMD / Qualcomm firmware, bootloader, modem, partition images or vendor binaries. Those are proprietary
  and cannot be redistributed; anyone building jgmDroid must extract them from their own phone.
- No Android (AOSP) source or builds. Android Open Source Project code is Apache-2.0 / GPL-2.0 and is fetched from
  android.googlesource.com by whoever builds it.
- No photos, logs or identifiers from any particular phone.

## Third-party material that is included
- `branding/fonts/DejaVuSans-Bold.ttf`: DejaVu fonts, Bitstream Vera licence plus public-domain changes
  (see `branding/fonts/DejaVu-LICENSE.txt`). Used to draw the wordmark in the boot animation.

## Trademarks and attribution
- "Android" is a trademark of Google LLC. "Android 8.1 Go" is used only to say which Android version this targets.
  The droid in the boot animation is drawn by hand for this project, but it is modelled on the Android robot, so it
  carries Google's attribution line: "The Android robot is reproduced or modified from work created and shared by Google
  and used according to terms described in the Creative Commons 3.0 Attribution License."
  Guidelines: https://developer.android.com/distribute/marketing-tools/brand-guidelines
- "Nokia" and "8110 4G" are trademarks of their owners and are used only to name the supported phone.
- "Droid" is a trademark of Lucasfilm Ltd. (licensed to Verizon). This project is not affiliated with any of the
  companies named here.
