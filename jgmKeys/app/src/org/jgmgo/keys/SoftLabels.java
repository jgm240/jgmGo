package org.jgmgo.keys;

/** Pure logic: what to print on the thin bar above the soft keys, like an old feature phone. */
public final class SoftLabels {
    public final String left, center, right;
    private SoftLabels(String l, String c, String r) { left = l; center = c; right = r; }

    public static SoftLabels forState(boolean editing, boolean pointer, boolean hints, String itemLabel, String typingMode) {
        if (hints) return new SoftLabels("Recents", "Press a number", "Cancel");
        if (pointer) return new SoftLabels("Recents", "Tap", "Back");
        if (editing) return new SoftLabels("Recents", (typingMode == null || typingMode.isEmpty() ? "Abc" : typingMode) + "  (#)", "Back");
        String c = itemLabel == null || itemLabel.isEmpty() ? "Select" : shorten(itemLabel, 18);
        return new SoftLabels("Recents", c, "Back");
    }
    static String shorten(String s, int max) { return s.length() <= max ? s : s.substring(0, max - 1) + "\u2026"; }
}
