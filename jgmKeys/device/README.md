# jgmKeys: use the whole phone with the keypad

## Parts
- `app/`: system app (`packages/apps/JgmKeys`).
  - `KeyNavService`: accessibility service. Arrows move a highlight between clickable items, OK presses, edge of the list
    scrolls. Soft-left = recents (long: notifications), soft-right = back (long: quick settings), Home key = home,
    clear key = back (backspace in text). Long-press `*` toggles a free pointer for screens with nothing to select.
  - `T9Ime`: multi-tap keyboard (2 = abc ...), `#` cycles Abc / abc / ABC / 123, `0` = space.
  - `BootReceiver`: turns both on at first boot (a touchscreen-less phone can't do it in Settings).
- `device/keylayout/matrix_keypad.kl`: key map for the keypad.

## Device tree needs (not done yet)
- `PRODUCT_COPY_FILES += frameworks/native/data/etc/android.hardware.faketouch.xml:system/etc/permissions/android.hardware.faketouch.xml`
  so apps that insist on a touchscreen still run.
- `ro.setupwizard.mode=DISABLED` (the setup wizard needs touch), and a framework overlay with `config_showNavigationBar=false`.
- Check that the lock screen can be dismissed with the keypad; it uses the number keys for a PIN.

## Tests
`test/Tests.java` covers the navigation, key map and multi-tap logic (plain Java, no phone needed):
`javac -d out app/src/org/jgmgo/keys/{NavModel,KeyMap,MultiTap}.java test/Tests.java && java -cp out Tests`.
The Android classes are compile-checked against the SDK jar once the AOSP tree is available.
