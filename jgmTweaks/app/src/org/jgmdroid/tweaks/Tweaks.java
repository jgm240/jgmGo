package org.jgmdroid.tweaks;

/** The settings jgmTweaks can change, their allowed values, and how they are written to system properties. */
public final class Tweaks {
    private Tweaks() {}
    public static final String PROP_ZRAM = "persist.jgm.zram_mb";            // 0 = zram off
    public static final String PROP_SWAPPINESS = "persist.jgm.swappiness";   // 0..100
    public static final String PROP_SWAPFILE = "persist.jgm.swapfile_mb";    // 0 = no swap file
    public static final String PROP_SEQ = "persist.jgm.apply_seq";           // bumped to make init run the apply script

    /** Allowed choices. The phone has about 400 MB of usable RAM, so zram above 512 MB only costs CPU. */
    public static final int[] ZRAM_MB = {0, 64, 128, 192, 256, 320, 384, 512};
    public static final int[] SWAPFILE_MB = {0, 128, 256, 512, 1024};
    public static final int SWAPPINESS_MIN = 0, SWAPPINESS_MAX = 100, SWAPPINESS_STEP = 10;

    public static final int DEFAULT_ZRAM = 256, DEFAULT_SWAPPINESS = 60, DEFAULT_SWAPFILE = 0;

    /** Nearest allowed value (so a hand-edited property can never produce something odd). */
    public static int snap(int value, int[] allowed) {
        int best = allowed[0];
        for (int a : allowed) if (Math.abs(a - value) < Math.abs(best - value)) best = a;
        return best;
    }
    public static int clampSwappiness(int v) { return Math.max(SWAPPINESS_MIN, Math.min(SWAPPINESS_MAX, v)); }
    public static int step(int[] allowed, int current, int dir) {
        int i = 0;
        for (int k = 0; k < allowed.length; k++) if (allowed[k] == current) i = k;
        return allowed[Math.max(0, Math.min(allowed.length - 1, i + dir))];
    }
    public static String label(int mb) { return mb == 0 ? "Off" : (mb >= 1024 ? (mb / 1024) + " GB" : mb + " MB"); }
}
