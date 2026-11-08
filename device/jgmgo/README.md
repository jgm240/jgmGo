# Device files for jgmGo (Nokia 8110 4G)
`device.mk` is the product makefile. It builds on Google's 512 MB "Go" defaults and adds jgmKeys, jgmTweaks, jgmLauncher
and jgmVoice. The files it copies are gathered from the app folders by `tools/stage-device.sh`.

## Still to do (needs the real phone's files)
- Kernel: the phone's own 3.10.49 kernel, plus a `BoardConfig.mk` (memory map, boot image offsets, partition sizes).
- Vendor files for graphics, audio, camera, modem and sensors, extracted from your own phone (they are proprietary and are not in this repo).
- A screen driver check: the panel is a 240x320 SPI display on the older MDP3 path; a plain framebuffer works (see `experiments/fbtest`).
- A framework overlay with `config_showNavigationBar=false`.

## Voice assistant extras (not in this repo)
- `libvosk.so` (Vosk 0.3.45, 32-bit ARM, Apache-2.0) into `vendor/jgmgo/voice/libvosk.so`
- an English model (for example `vosk-model-small-en-us`, Apache-2.0) into `vendor/jgmgo/voice/model/`
