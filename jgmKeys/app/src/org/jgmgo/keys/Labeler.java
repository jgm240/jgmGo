package org.jgmgo.keys;

/** Pure logic: the words spoken or shown for the highlighted item. */
public final class Labeler {
    private Labeler() {}
    public static String labelOf(CharSequence text, CharSequence description, String className, boolean checkable, boolean checked) {
        String t = clean(text), d = clean(description);
        String main = !t.isEmpty() ? t : !d.isEmpty() ? d : kind(className);
        if (checkable) main += checked ? ", on" : ", off";
        return main;
    }
    private static String clean(CharSequence s) { return s == null ? "" : s.toString().replaceAll("\\s+", " ").trim(); }
    private static String kind(String cls) {
        if (cls == null) return "item";
        String c = cls.substring(cls.lastIndexOf('.') + 1);
        if (c.contains("Button")) return "button";
        if (c.contains("Edit")) return "text field";
        if (c.contains("Switch") || c.contains("Check") || c.contains("Radio")) return "switch";
        if (c.contains("Image")) return "image";
        return "item";
    }
}
