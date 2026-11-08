import org.jgmgo.keys.*;
import org.jgmgo.keys.NavModel.*;
import java.util.*;

public class Tests {
    static int n = 0;
    static void eq(Object a, Object b, String what) { if (!String.valueOf(a).equals(String.valueOf(b))) throw new Error(what + ": got " + a + " expected " + b); n++; }
    static R r(int l, int t, int w, int h) { return new R(l, t, l + w, t + h); }

    public static void main(String[] x) {
        // 3x3 grid of 60x40 buttons, index = row*3+col
        List<R> g = new ArrayList<>();
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) g.add(r(col * 80, row * 60, 60, 40));
        eq(NavModel.next(g, 4, Dir.UP), 1, "grid up"); eq(NavModel.next(g, 4, Dir.DOWN), 7, "grid down");
        eq(NavModel.next(g, 4, Dir.LEFT), 3, "grid left"); eq(NavModel.next(g, 4, Dir.RIGHT), 5, "grid right");
        eq(NavModel.next(g, 0, Dir.UP), -1, "top-left up is the edge"); eq(NavModel.next(g, 8, Dir.RIGHT), -1, "bottom-right right is the edge");
        eq(NavModel.next(g, 0, Dir.DOWN), 3, "corner down"); eq(NavModel.next(g, 2, Dir.LEFT), 1, "corner left");
        eq(NavModel.next(g, -1, Dir.DOWN), 0, "start top-left"); eq(NavModel.next(g, -1, Dir.UP), 8, "start bottom-right");
        eq(NavModel.next(new ArrayList<R>(), -1, Dir.DOWN), -1, "empty");
        // a list: wide rows stacked, with a small button on the right of row 1
        List<R> l = Arrays.asList(r(0, 0, 200, 40), r(0, 50, 200, 40), r(150, 100, 40, 30), r(0, 140, 200, 40));
        eq(NavModel.next(l, 0, Dir.DOWN), 1, "list down"); eq(NavModel.next(l, 1, Dir.DOWN), 2, "list down to small button");
        eq(NavModel.next(l, 2, Dir.DOWN), 3, "from button down to row"); eq(NavModel.next(l, 3, Dir.UP), 2, "up to the button");
        // off-centre targets: prefer the one that lines up
        List<R> o = Arrays.asList(r(100, 0, 60, 40), r(0, 60, 60, 40), r(100, 60, 60, 40));
        eq(NavModel.next(o, 0, Dir.DOWN), 2, "prefers the aligned one");

        // key map
        eq(KeyMap.decide(KeyMap.SOFT_RIGHT, false, false, false), "BACK", "soft right back");
        eq(KeyMap.decide(KeyMap.SOFT_RIGHT, true, false, false), "QUICK_SETTINGS", "soft right long");
        eq(KeyMap.decide(KeyMap.SOFT_LEFT, false, false, false), "RECENTS", "soft left"); eq(KeyMap.decide(KeyMap.SOFT_LEFT, true, false, false), "NOTIFICATIONS", "soft left long");
        eq(KeyMap.decide(KeyMap.ENTER, false, false, false), "CLICK", "ok"); eq(KeyMap.decide(KeyMap.DPAD_CENTER, true, false, false), "LONG_CLICK", "ok long");
        eq(KeyMap.decide(KeyMap.DPAD_LEFT, false, true, false), "PASS", "left in a text field moves the cursor");
        eq(KeyMap.decide(KeyMap.DPAD_LEFT, false, true, true), "NAV_LEFT", "left in pointer mode moves the pointer");
        eq(KeyMap.decide(KeyMap.DPAD_UP, false, true, false), "NAV_UP", "up still navigates in a text field");
        eq(KeyMap.decide(KeyMap.DEL, false, true, false), "PASS", "clear key edits text"); eq(KeyMap.decide(KeyMap.DEL, false, false, false), "BACK", "clear key goes back");
        eq(KeyMap.decide(KeyMap.STAR, true, false, false), "POINTER_TOGGLE", "star long toggles pointer"); eq(KeyMap.decide(8, false, false, false), "PASS", "digits pass");

        // multi-tap: "hello world" with the right timing
        MultiTap m = new MultiTap(); StringBuilder out = new StringBuilder(); long t = 0;
        // h e l l o, space, w o r l d  (key, taps)
        int[][] word = {{4,2},{3,2},{5,3},{5,3},{6,3},{0,1},{9,1},{6,3},{7,3},{5,3},{3,1}};
        for (int[] kt : word) {
            for (int k = 0; k < kt[1]; k++) { MultiTap.Edit e = m.press(kt[0], t); if (e.deleteBefore > 0) out.setLength(out.length() - e.deleteBefore); out.append(e.insert); t += 200; }
            t += 1200;      // pause: the character is final
            m.commitIfExpired(t);
        }
        eq(out, "Hello world", "hello world typed with auto capital");
        // cycling and wrap-around
        MultiTap c = new MultiTap(); c.cycleMode();          // CAP_NEXT -> LOWER
        StringBuilder s = new StringBuilder(); long q = 0;
        for (int k = 0; k < 4; k++) { MultiTap.Edit e = c.press(2, q); if (e.deleteBefore > 0) s.setLength(s.length() - e.deleteBefore); s.append(e.insert); q += 100; }
        eq(s, "2", "four taps on 2 wrap to the digit 2");
        MultiTap d = new MultiTap(); d.cycleMode(); d.cycleMode(); d.cycleMode();   // Abc -> abc -> ABC -> 123
        eq(d.mode(), "NUM", "three presses of # reach 123");
        eq(d.press(7, 0).insert, "7", "number mode types digits"); eq(d.press(7, 50).deleteBefore, 0, "number mode never replaces");
        d.cycleMode(); eq(d.mode(), "CAP_NEXT", "one more returns to Abc");
        MultiTap tmo = new MultiTap(); tmo.cycleMode();
        tmo.press(2, 0); MultiTap.Edit after = tmo.press(2, 2000);
        eq(after.deleteBefore, 0, "a slow second tap starts a new letter"); eq(after.insert, "a", "new letter");
        MultiTap dif = new MultiTap(); dif.cycleMode(); dif.press(2, 0); eq(dif.press(3, 100).deleteBefore, 0, "a different key starts a new letter");
        MultiTap up = new MultiTap(); eq(up.press(4, 0).insert, "G", "Abc mode: first letter is a capital"); up.commit(); eq(up.press(4, 2000).insert, "g", "then lower case");
        System.out.println(n + " checks passed");
    }
}
