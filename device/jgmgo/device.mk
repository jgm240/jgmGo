# jgmGo device makefile for the Nokia 8110 4G (msm8909). Incomplete on purpose: the kernel, BoardConfig and the vendor
# blobs still have to be added; see README.md in this folder.
$(call inherit-product, build/target/product/go_defaults_512.mk)

PRODUCT_NAME := jgmgo
PRODUCT_DEVICE := jgmgo_8110
PRODUCT_BRAND := jgmGo
PRODUCT_MODEL := jgmGo
PRODUCT_MANUFACTURER := unofficial

# Our apps. jgmLauncher replaces the stock launcher; jgmKeys makes the whole phone usable without a touchscreen.
PRODUCT_PACKAGES += JgmKeys JgmTweaks JgmLauncher JgmVoice libjgmvosk JgmKeysLogic
# Small, light base apps (names are the module names in the AOSP 8.1 tree)
PRODUCT_PACKAGES += Settings Dialer Messaging Contacts DeskClock ExactCalculator Camera2 Browser2

# No touchscreen: apps that insist on one still run with "fake touch" (jgmKeys supplies the pointer)
PRODUCT_COPY_FILES += frameworks/native/data/etc/android.hardware.faketouch.xml:system/etc/permissions/android.hardware.faketouch.xml
# Key layouts: keypad, and the two hall sensors of the slider
PRODUCT_COPY_FILES += \
    device/jgmgo/keylayout/matrix_keypad.kl:system/usr/keylayout/matrix_keypad.kl \
    device/jgmgo/keylayout/hall_sensor0.kl:system/usr/keylayout/hall_sensor0.kl \
    device/jgmgo/keylayout/hall_sensor1.kl:system/usr/keylayout/hall_sensor1.kl
# jgmTweaks: root helper and its init script
PRODUCT_COPY_FILES += \
    device/jgmgo/jgmtweaks-apply.sh:system/bin/jgmtweaks-apply.sh \
    device/jgmgo/init.jgmtweaks.rc:system/etc/init/init.jgmtweaks.rc
PRODUCT_COPY_FILES += device/jgmgo/privapp-permissions-jgmgo.xml:system/etc/permissions/privapp-permissions-jgmgo.xml
BOARD_SEPOLICY_DIRS += device/jgmgo/sepolicy

# Voice: Vosk's library and a small English model are NOT in this repository (they are separate downloads, see README.md)
ifneq ($(wildcard vendor/jgmgo/voice/libvosk.so),)
PRODUCT_COPY_FILES += vendor/jgmgo/voice/libvosk.so:system/lib/libvosk.so
PRODUCT_COPY_FILES += $(call find-copy-subdir-files,*,vendor/jgmgo/voice/model,system/etc/jgmvoice/model)
endif

PRODUCT_PROPERTY_OVERRIDES += \
    ro.setupwizard.mode=DISABLED \
    persist.jgm.zram_mb=256 \
    persist.jgm.swappiness=60 \
    dalvik.vm.heapstartsize=2m \
    dalvik.vm.heapgrowthlimit=64m \
    dalvik.vm.heapsize=128m \
    ro.config.max_starting_bg=2 \
    ro.config.fha_enable=true \
    ro.sf.lcd_density=120
