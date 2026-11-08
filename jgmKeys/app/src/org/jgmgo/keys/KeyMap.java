package org.jgmgo.keys;

/** Pure logic: what should a hardware key do? (Android key codes are repeated here as numbers so this can be tested off-device.) */
public final class KeyMap {
    private KeyMap() {}
    public static final int DPAD_UP = 19, DPAD_DOWN = 20, DPAD_LEFT = 21, DPAD_RIGHT = 22, DPAD_CENTER = 23, ENTER = 66,
            SOFT_LEFT = 1, SOFT_RIGHT = 2, HOME = 3, BACK = 4, STAR = 17, POUND = 18, DEL = 67;

    public enum Action { PASS, NAV_UP, NAV_DOWN, NAV_LEFT, NAV_RIGHT, CLICK, LONG_CLICK, BACK, HOME, RECENTS, NOTIFICATIONS, QUICK_SETTINGS, POINTER_TOGGLE }

    /**
     * @param editing   a text field has focus: left/right must move the text cursor, so they are passed on
     * @param pointer   pointer (free cursor) mode is on: the arrows move the cursor instead of the highlight
     */
    public static Action decide(int keyCode, boolean longPress, boolean editing, boolean pointer) {
        switch (keyCode) {
            case SOFT_RIGHT: return longPress ? Action.QUICK_SETTINGS : Action.BACK;
            case SOFT_LEFT:  return longPress ? Action.NOTIFICATIONS : Action.RECENTS;
            case HOME:       return Action.HOME;
            case DEL:        return editing ? Action.PASS : Action.BACK;      // the clear key: backspace in text, otherwise back
            case STAR:       return longPress ? Action.POINTER_TOGGLE : Action.PASS;
            case DPAD_CENTER:
            case ENTER:      return longPress ? Action.LONG_CLICK : Action.CLICK;
            case DPAD_UP:    return Action.NAV_UP;
            case DPAD_DOWN:  return Action.NAV_DOWN;
            case DPAD_LEFT:  return editing && !pointer ? Action.PASS : Action.NAV_LEFT;
            case DPAD_RIGHT: return editing && !pointer ? Action.PASS : Action.NAV_RIGHT;
            default:         return Action.PASS;     // digits, call, volume, power... are handled by the system as usual
        }
    }
}
