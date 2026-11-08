package org.jgmgo.launcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure logic: which app sits on which number key (2..9). Key 1 = voicemail, 0 = all apps, both fixed. */
public final class Favorites {
    private final Map<Integer, String> slots = new HashMap<>();      // digit -> component name ("package/Class")

    public static boolean assignable(int digit) { return digit >= 2 && digit <= 9; }
    public String get(int digit) { return slots.get(digit); }
    public void set(int digit, String component) {
        if (!assignable(digit)) throw new IllegalArgumentException("digit " + digit);
        for (int d = 2; d <= 9; d++) if (component.equals(slots.get(d))) slots.remove(d);     // one app, one key
        slots.put(digit, component);
    }
    public void clear(int digit) { slots.remove(digit); }

    /** Fill empty keys with the first installed apps from a preferred list (package-name fragments, in order). */
    public void fillDefaults(List<String> installed, String[] preferred) {
        for (String want : preferred) {
            int free = -1;
            for (int d = 2; d <= 9; d++) if (!slots.containsKey(d)) { free = d; break; }
            if (free < 0) return;
            for (String c : installed) if (c.contains(want) && !slots.containsValue(c)) { slots.put(free, c); break; }
        }
    }

    /** "2=pkg/Cls;3=pkg/Cls" for SharedPreferences. */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        for (int d = 2; d <= 9; d++) if (slots.containsKey(d)) { if (sb.length() > 0) sb.append(';'); sb.append(d).append('=').append(slots.get(d)); }
        return sb.toString();
    }
    public static Favorites parse(String s) {
        Favorites f = new Favorites();
        if (s == null) return f;
        for (String part : s.split(";")) {
            int eq = part.indexOf('=');
            if (eq < 1) continue;
            try { int d = Integer.parseInt(part.substring(0, eq)); if (assignable(d)) f.slots.put(d, part.substring(eq + 1)); } catch (NumberFormatException ignored) { /* skip */ }
        }
        return f;
    }
    public List<String> components() { List<String> l = new ArrayList<>(); for (int d = 2; d <= 9; d++) if (slots.containsKey(d)) l.add(slots.get(d)); return l; }
}
