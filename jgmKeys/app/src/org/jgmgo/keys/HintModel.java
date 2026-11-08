package org.jgmgo.keys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Pure logic: "number hints". Every clickable item gets a digit; 10 per page (1..9 then 0), in reading order. */
public final class HintModel {
    public static final int PER_PAGE = 10;
    private final List<Integer> order = new ArrayList<>();      // item indexes in reading order

    public HintModel(final List<NavModel.R> rects) {
        for (int i = 0; i < rects.size(); i++) order.add(i);
        Collections.sort(order, new Comparator<Integer>() {
            @Override public int compare(Integer a, Integer b) {
                NavModel.R x = rects.get(a), y = rects.get(b);
                int rowX = (x.t + x.b) / 2 / 40, rowY = (y.t + y.b) / 2 / 40;     // items within ~40px of height share a row
                if (rowX != rowY) return Integer.compare(rowX, rowY);
                return Integer.compare(x.l, y.l);
            }
        });
    }
    public int pageCount() { return Math.max(1, (order.size() + PER_PAGE - 1) / PER_PAGE); }
    /** Item indexes shown on a page, in digit order (1,2,...,9,0). */
    public List<Integer> page(int p) {
        int from = Math.max(0, p) * PER_PAGE, to = Math.min(order.size(), from + PER_PAGE);
        return from >= to ? new ArrayList<Integer>() : new ArrayList<>(order.subList(from, to));
    }
    /** Which item does this digit pick on this page? -1 if none. */
    public int pick(int page, int digit) {
        int slot = digit == 0 ? 9 : digit - 1;
        List<Integer> p = page(page);
        return slot >= 0 && slot < p.size() ? p.get(slot) : -1;
    }
    /** The digit shown on the badge of slot k (0-based). */
    public static int digitOf(int slot) { return slot == 9 ? 0 : slot + 1; }
}
