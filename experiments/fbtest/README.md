# fbtest: can a bare kernel drive the Nokia 8110 4G screen and keypad?
A tiny static `/init` used as the ramdisk of a test boot image. It lights the backlight, draws colour bars on the
240x320 RGB565 framebuffer (`/dev/graphics/fb0`, 240x640 virtual, two pages), reads every input device and changes
the screen on each key press, logs to `/cache`, then reboots normally.
Build: `armv7a-linux-androideabi23-clang -static -O2 -o init fbtest.c`.
Result on the phone's own 3.10.49 kernel: the screen and the keypad (matrix_keypad, gpio-keys, qpnp_pon) both work.
