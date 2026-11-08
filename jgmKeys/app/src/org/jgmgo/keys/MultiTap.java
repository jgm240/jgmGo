package org.jgmgo.keys;

/** Pure logic: classic multi-tap typing for a phone keypad (press 2 once = a, twice = b ...). */
public final class MultiTap {
    public enum Mode { LOWER, CAP_NEXT, UPPER, NUM }
    public static final long TIMEOUT_MS = 900;

    /** Replace the last {@code deleteBefore} characters with {@code insert}. */
    public static final class Edit {
        public final int deleteBefore; public final String insert;
        Edit(int d, String i) { deleteBefore = d; insert = i; }
        @Override public String toString() { return "del" + deleteBefore + "+'" + insert + "'"; }
    }

    private static final String[] KEYS = {" 0", ".,?!'-@1", "abc2", "def3", "ghi4", "jkl5", "mno6", "pqrs7", "tuv8", "wxyz9"};
    private Mode mode = Mode.CAP_NEXT;
    private int lastKey = -1, index = 0;
    private long lastTime = 0;
    private boolean pending = false, pendingUpper = false;

    public Mode mode() { return mode; }
    public boolean hasPending() { return pending; }

    /** The '#' key: cycles Abc -> abc -> ABC -> 123. */
    public void cycleMode() {
        pending = false;
        switch (mode) {
            case CAP_NEXT: mode = Mode.LOWER; break;
            case LOWER: mode = Mode.UPPER; break;
            case UPPER: mode = Mode.NUM; break;
            default: mode = Mode.CAP_NEXT; break;
        }
    }

    /** A digit key 0-9 was pressed at time {@code now}. */
    public Edit press(int digit, long now) {
        if (digit < 0 || digit > 9) throw new IllegalArgumentException("digit");
        if (mode == Mode.NUM) { pending = false; return new Edit(0, String.valueOf(digit)); }
        String set = KEYS[digit];
        boolean repeat = pending && digit == lastKey && now - lastTime <= TIMEOUT_MS;
        Edit e;
        if (repeat) {
            index = (index + 1) % set.length();
            e = new Edit(1, shape(set.charAt(index), pendingUpper));
        } else {
            index = 0;
            boolean up = mode == Mode.UPPER || mode == Mode.CAP_NEXT;
            if (mode == Mode.CAP_NEXT) mode = Mode.LOWER;
            pendingUpper = up;
            e = new Edit(0, shape(set.charAt(0), up));
        }
        lastKey = digit; lastTime = now; pending = true;
        return e;
    }

    /** Called when time passes: true if the pending character is now final. */
    public boolean commitIfExpired(long now) {
        if (pending && now - lastTime > TIMEOUT_MS) { pending = false; return true; }
        return false;
    }
    public void commit() { pending = false; }
    /** After a space or a full stop, the next letter is capitalised (when auto-capitalising is on). */
    public void sentenceEnd() { if (mode == Mode.LOWER) mode = Mode.CAP_NEXT; }

    private static String shape(char c, boolean upper) { return String.valueOf(upper ? Character.toUpperCase(c) : c); }
}
