# jgmTweaks for jgmGo
- `app/`  the Android app (`packages/apps/JgmTweaks`): keypad-first screen for zram size, swappiness and an optional swap file.
- `device/jgmdroid/`  files for the device tree: `jgmtweaks-apply.sh` (installed to /system/bin), `init.jgmtweaks.rc`, `jgm_apply.te` (SELinux).
The app only sets `persist.jgm.*` properties. init starts the root script when `persist.jgm.apply_seq` changes, and at every boot.
