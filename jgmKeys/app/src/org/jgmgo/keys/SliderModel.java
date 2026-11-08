package org.jgmgo.keys;

/**
 * Pure logic: the Nokia 8110's slider. Its two hall sensors arrive as two different keys (the key layouts name them
 * F11 and F12). Which one means "open" is a setting, because it has to be confirmed on the real phone.
 */
public final class SliderModel {
    public enum State { UNKNOWN, OPEN, CLOSED }
    public enum Do { NOTHING, ANSWER_CALL, WAKE, END_CALL, SLEEP }

    private final int openKey, closedKey;
    private State state = State.UNKNOWN;
    public SliderModel(int openKey, int closedKey) { this.openKey = openKey; this.closedKey = closedKey; }
    public State state() { return state; }

    /** A hall-sensor key went down (magnet near) or up (magnet away). Returns the new state if it changed, else UNKNOWN. */
    public State onKey(int keyCode, boolean down) {
        State s = state;
        if (keyCode == openKey) s = down ? State.OPEN : State.CLOSED;
        else if (keyCode == closedKey) s = down ? State.CLOSED : State.OPEN;
        else return State.UNKNOWN;
        if (s == state) return State.UNKNOWN;
        state = s;
        return s;
    }

    public static Do decide(State changedTo, boolean ringing, boolean inCall) {
        if (changedTo == State.OPEN) return ringing ? Do.ANSWER_CALL : Do.WAKE;
        if (changedTo == State.CLOSED) return inCall ? Do.END_CALL : Do.SLEEP;
        return Do.NOTHING;
    }
}
