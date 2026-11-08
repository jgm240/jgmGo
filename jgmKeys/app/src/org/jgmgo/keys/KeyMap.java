package org.jgmgo.keys;

/** Pure logic: what should a hardware key do? (Android key codes are repeated as numbers so this can be tested off-device.) */
public final class KeyMap {
    private KeyMap() {}
    public static final int DPAD_UP = 19, DPAD_DOWN = 20, DPAD_LEFT = 21, DPAD_RIGHT = 22, DPAD_CENTER = 23, ENTER = 66,
            SOFT_LEFT = 1, SOFT_RIGHT = 2, HOME = 3, BACK = 4, STAR = 17, POUND = 18, DEL = 67, KEY_0 = 7, KEY_9 = 16;

    public enum Action { PASS, NAV_UP, NAV_DOWN, NAV_LEFT, NAV_RIGHT, CLICK, LONG_CLICK, BACK, HOME, RECENTS, NOTIFICATIONS,
        QUICK_SETTINGS, POINTER_TOGGLE, HINTS_TOGGLE, HINT_DIGIT, HINT_NEXT, HINT_EXIT, TYPE_AHEAD }

    /** What the screen is doing right now. */
    public static final class Ctx {
        public boolean editing;   // a text field has focus: left/right must move the text cursor
        public boolean pointer;   // free pointer mode
        public boolean hints;     // number-hint mode
        public boolean list;      // the highlighted item is inside a long scrolling list
        public String pkg = "";   // package of the foreground app
        public Ctx editing(boolean v) { editing = v; return this; }
        public Ctx pointer(boolean v) { pointer = v; return this; }
        public Ctx hints(boolean v) { hints = v; return this; }
        public Ctx list(boolean v) { list = v; return this; }
        public Ctx pkg(String v) { pkg = v; return this; }
    }

    /** Apps where the number keys mean numbers, so they are never taken for type-ahead. */
    public static boolean digitsBelongToApp(String pkg) {
        return pkg != null && (pkg.contains("dialer") || pkg.contains("incallui") || pkg.contains("calculator")
                || pkg.contains("contacts") || pkg.startsWith("org.jgmgo.launcher"));
    }
    public static boolean isDigit(int code) { return code >= KEY_0 && code <= KEY_9; }
    public static int digitOf(int code) { return code - KEY_0; }

    public static Action decide(int code, boolean longPress, Ctx c) {
        if (c.hints) {
            if (isDigit(code)) return Action.HINT_DIGIT;
            switch (code) {
                case STAR: return Action.HINT_NEXT;
                case POUND: case SOFT_RIGHT: case DEL: case ENTER: case DPAD_CENTER: return Action.HINT_EXIT;
                case HOME: return Action.HOME;
                case SOFT_LEFT: return Action.RECENTS;
                case DPAD_UP: return Action.NAV_UP;
                case DPAD_DOWN: return Action.NAV_DOWN;
                case DPAD_LEFT: return Action.NAV_LEFT;
                case DPAD_RIGHT: return Action.NAV_RIGHT;
                default: return Action.PASS;
            }
        }
        switch (code) {
            case SOFT_RIGHT: return longPress ? Action.QUICK_SETTINGS : Action.BACK;
            case SOFT_LEFT:  return longPress ? Action.NOTIFICATIONS : Action.RECENTS;
            case HOME:       return Action.HOME;
            case DEL:        return c.editing ? Action.PASS : Action.BACK;      // clear key: backspace in text, otherwise back
            case STAR:       return longPress && !c.editing ? Action.POINTER_TOGGLE : Action.PASS;
            case POUND:      return longPress && !c.editing ? Action.HINTS_TOGGLE : Action.PASS;
            case DPAD_CENTER:
            case ENTER:      return longPress ? Action.LONG_CLICK : Action.CLICK;
            case DPAD_UP:    return Action.NAV_UP;
            case DPAD_DOWN:  return Action.NAV_DOWN;
            case DPAD_LEFT:  return c.editing && !c.pointer ? Action.PASS : Action.NAV_LEFT;
            case DPAD_RIGHT: return c.editing && !c.pointer ? Action.PASS : Action.NAV_RIGHT;
            default:
                if (isDigit(code) && !longPress && !c.editing && c.list && !digitsBelongToApp(c.pkg)
                        && TypeAhead.isLetterKey(digitOf(code))) return Action.TYPE_AHEAD;
                return Action.PASS;      // digits, call, volume, power... are handled by the system as usual
        }
    }
}
