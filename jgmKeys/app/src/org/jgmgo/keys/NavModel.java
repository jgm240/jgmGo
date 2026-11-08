package org.jgmgo.keys;

import java.util.List;

/** Pure logic: given the on-screen rectangles of clickable things, which one is next in a direction? */
public final class NavModel {
    private NavModel() {}
    public enum Dir { UP, DOWN, LEFT, RIGHT }

    public static final class R {
        public final int l, t, r, b;
        public R(int l, int t, int r, int b) { this.l = l; this.t = t; this.r = r; this.b = b; }
        int cx() { return (l + r) / 2; }
        int cy() { return (t + b) / 2; }
    }

    /** Index of the next rectangle, or -1 if there is nothing further in that direction. cur = -1 means "nothing selected yet". */
    public static int next(List<R> rects, int cur, Dir d) {
        if (rects.isEmpty()) return -1;
        if (cur < 0 || cur >= rects.size()) return first(rects, d);
        R s = rects.get(cur);
        int best = -1;
        long bestScore = Long.MAX_VALUE;
        for (int i = 0; i < rects.size(); i++) {
            if (i == cur) continue;
            R c = rects.get(i);
            if (!beyond(s, c, d)) continue;
            long major, minor;
            switch (d) {
                case DOWN:  major = Math.max(0, c.t - s.b); minor = Math.abs(c.cx() - s.cx()); if (overlapX(s, c)) minor /= 2; break;
                case UP:    major = Math.max(0, s.t - c.b); minor = Math.abs(c.cx() - s.cx()); if (overlapX(s, c)) minor /= 2; break;
                case RIGHT: major = Math.max(0, c.l - s.r); minor = Math.abs(c.cy() - s.cy()); if (overlapY(s, c)) minor /= 2; break;
                default:    major = Math.max(0, s.l - c.r); minor = Math.abs(c.cy() - s.cy()); if (overlapY(s, c)) minor /= 2; break;
            }
            long score = 13 * major * major + minor * minor;      // same weighting idea as Android's FocusFinder
            if (score < bestScore) { bestScore = score; best = i; }
        }
        return best;
    }

    /** Where to start when nothing is selected: top-left for DOWN/RIGHT, bottom-right for UP/LEFT. */
    static int first(List<R> rects, Dir d) {
        int best = 0;
        for (int i = 1; i < rects.size(); i++) {
            R a = rects.get(i), b = rects.get(best);
            boolean forward = d == Dir.DOWN || d == Dir.RIGHT;
            boolean earlier = a.t != b.t ? a.t < b.t : a.l < b.l;
            boolean later = a.b != b.b ? a.b > b.b : a.r > b.r;
            if (forward ? earlier : later) best = i;
        }
        return best;
    }
    private static boolean beyond(R s, R c, Dir d) {
        switch (d) {
            case DOWN:  return c.cy() > s.cy() && c.b > s.b;
            case UP:    return c.cy() < s.cy() && c.t < s.t;
            case RIGHT: return c.cx() > s.cx() && c.r > s.r;
            default:    return c.cx() < s.cx() && c.l < s.l;
        }
    }
    private static boolean overlapX(R a, R b) { return a.l < b.r && b.l < a.r; }
    private static boolean overlapY(R a, R b) { return a.t < b.b && b.t < a.b; }
}
