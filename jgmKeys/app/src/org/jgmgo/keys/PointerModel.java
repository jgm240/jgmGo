package org.jgmgo.keys;

import java.util.List;

/** Pure logic: the free pointer. It accelerates while an arrow is held and snaps to the middle of a clickable item. */
public final class PointerModel {
    private PointerModel() {}
    public static int stepSize(int repeat) { return repeat < 3 ? 14 : repeat < 8 ? 28 : repeat < 16 ? 52 : 90; }

    /** Returns {x, y} after one move. If the new spot lies inside an item, the pointer snaps to that item's centre. */
    public static int[] move(int x, int y, NavModel.Dir d, int repeat, List<NavModel.R> items, int w, int h) {
        int s = stepSize(repeat);
        switch (d) { case UP: y -= s; break; case DOWN: y += s; break; case LEFT: x -= s; break; default: x += s; break; }
        x = Math.max(0, Math.min(w - 1, x)); y = Math.max(0, Math.min(h - 1, y));
        NavModel.R hit = null; long area = Long.MAX_VALUE;
        for (NavModel.R r : items) {
            if (x >= r.l && x < r.r && y >= r.t && y < r.b) {
                long a = (long) (r.r - r.l) * (r.b - r.t);
                if (a < area) { area = a; hit = r; }        // the smallest item under the pointer wins
            }
        }
        if (hit != null) { x = (hit.l + hit.r) / 2; y = (hit.t + hit.b) / 2; }
        return new int[] {x, y};
    }
}
