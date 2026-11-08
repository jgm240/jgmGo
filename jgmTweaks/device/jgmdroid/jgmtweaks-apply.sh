#!/system/bin/sh
# Runs as root from init. Reads the persist.jgm.* properties and applies zram size, swappiness and an optional swap file.
ZRAM_MB=$(getprop persist.jgm.zram_mb);        [ -z "$ZRAM_MB" ] && ZRAM_MB=256
SWAPPINESS=$(getprop persist.jgm.swappiness);  [ -z "$SWAPPINESS" ] && SWAPPINESS=60
SWAPFILE_MB=$(getprop persist.jgm.swapfile_mb); [ -z "$SWAPFILE_MB" ] && SWAPFILE_MB=0
Z=/dev/block/zram0
SF=/data/jgm.swap
result=""

# --- zram: the swap device must be off and reset before its size can change
if [ -b $Z ]; then
  swapoff $Z 2>/dev/null
  echo 1 > /sys/block/zram0/reset 2>/dev/null
  if [ "$ZRAM_MB" -gt 0 ]; then
    echo $((ZRAM_MB * 1024 * 1024)) > /sys/block/zram0/disksize
    mkswap $Z >/dev/null 2>&1
    if swapon $Z 2>/dev/null; then result="zram ${ZRAM_MB} MB"; else result="zram failed"; fi
  else
    result="zram off"
  fi
else
  result="no zram device"
fi

# --- swap file on /data (lower priority than zram so it is only used when zram is full)
swapoff $SF 2>/dev/null
if [ "$SWAPFILE_MB" -gt 0 ]; then
  CUR=0; [ -f $SF ] && CUR=$(( $(stat -c %s $SF) / 1048576 ))
  if [ "$CUR" != "$SWAPFILE_MB" ]; then
    rm -f $SF
    dd if=/dev/zero of=$SF bs=1048576 count=$SWAPFILE_MB 2>/dev/null
    chmod 600 $SF
    mkswap $SF >/dev/null 2>&1
  fi
  if swapon -p 0 $SF 2>/dev/null; then result="$result, file ${SWAPFILE_MB} MB"; else result="$result, file failed"; fi
else
  rm -f $SF
fi

# --- swappiness
echo "$SWAPPINESS" > /proc/sys/vm/swappiness 2>/dev/null && result="$result, swappiness $SWAPPINESS"
setprop sys.jgm.apply_result "$result"
log -t jgmtweaks "$result"
