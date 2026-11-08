package org.jgmgo.keys;

/** The keyboard and the navigator run in the same process; this lets the soft-key bar show the current typing mode. */
public final class ModeState {
    private ModeState() {}
    public static volatile String typingMode = "Abc";
}
