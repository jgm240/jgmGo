import org.jgmgo.keys.*;
import org.jgmgo.keys.NavModel.*;
import java.util.*;

public class Tests {
    static int n = 0;
    static void eq(Object a, Object b, String what) { if (!String.valueOf(a).equals(String.valueOf(b))) throw new Error(what + ": got " + a + " expected " + b); n++; }
    static R r(int l, int t, int w, int h) { return new R(l, t, l + w, t + h); }
    static KeyMap.Ctx cx() { return new KeyMap.Ctx(); }

    public static void main(String[] x) {
        navigation(); keys(); typing(); predictive(); hints(); labels(); pointer(); typeAhead(); slider(); labeler();
        System.out.println(n + " checks passed");
    }

    static void navigation() {
        List<R> g = new ArrayList<>();
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) g.add(r(col * 80, row * 60, 60, 40));
        eq(NavModel.next(g, 4, Dir.UP), 1, "grid up"); eq(NavModel.next(g, 4, Dir.DOWN), 7, "grid down");
        eq(NavModel.next(g, 4, Dir.LEFT), 3, "grid left"); eq(NavModel.next(g, 4, Dir.RIGHT), 5, "grid right");
        eq(NavModel.next(g, 0, Dir.UP), -1, "edge up"); eq(NavModel.next(g, 8, Dir.RIGHT), -1, "edge right");
        eq(NavModel.next(g, -1, Dir.DOWN), 0, "start top-left"); eq(NavModel.next(g, -1, Dir.UP), 8, "start bottom-right");
        eq(NavModel.next(new ArrayList<R>(), -1, Dir.DOWN), -1, "empty");
        List<R> l = Arrays.asList(r(0, 0, 200, 40), r(0, 50, 200, 40), r(150, 100, 40, 30), r(0, 140, 200, 40));
        eq(NavModel.next(l, 1, Dir.DOWN), 2, "list down to small button"); eq(NavModel.next(l, 3, Dir.UP), 2, "up to the button");
        List<R> o = Arrays.asList(r(100, 0, 60, 40), r(0, 60, 60, 40), r(100, 60, 60, 40));
        eq(NavModel.next(o, 0, Dir.DOWN), 2, "prefers the aligned one");
    }

    static void keys() {
        eq(KeyMap.decide(KeyMap.SOFT_RIGHT, false, cx()), "BACK", "soft right"); eq(KeyMap.decide(KeyMap.SOFT_RIGHT, true, cx()), "QUICK_SETTINGS", "soft right long");
        eq(KeyMap.decide(KeyMap.SOFT_LEFT, false, cx()), "RECENTS", "soft left"); eq(KeyMap.decide(KeyMap.SOFT_LEFT, true, cx()), "NOTIFICATIONS", "soft left long");
        eq(KeyMap.decide(KeyMap.ENTER, false, cx()), "CLICK", "ok"); eq(KeyMap.decide(KeyMap.DPAD_CENTER, true, cx()), "LONG_CLICK", "ok long");
        eq(KeyMap.decide(KeyMap.DPAD_LEFT, false, cx().editing(true)), "PASS", "left in text");
        eq(KeyMap.decide(KeyMap.DPAD_LEFT, false, cx().editing(true).pointer(true)), "NAV_LEFT", "left in pointer mode");
        eq(KeyMap.decide(KeyMap.DPAD_UP, false, cx().editing(true)), "NAV_UP", "up in text");
        eq(KeyMap.decide(KeyMap.DEL, false, cx().editing(true)), "PASS", "clear edits"); eq(KeyMap.decide(KeyMap.DEL, false, cx()), "BACK", "clear = back");
        eq(KeyMap.decide(KeyMap.STAR, true, cx()), "POINTER_TOGGLE", "star long"); eq(KeyMap.decide(KeyMap.STAR, true, cx().editing(true)), "PASS", "star long in text");
        eq(KeyMap.decide(KeyMap.POUND, true, cx()), "HINTS_TOGGLE", "pound long = hints"); eq(KeyMap.decide(KeyMap.POUND, false, cx()), "PASS", "pound short passes");
        eq(KeyMap.decide(KeyMap.POUND, true, cx().editing(true)), "PASS", "pound long in text stays with the keyboard");
        // hint mode
        KeyMap.Ctx h = cx().hints(true);
        eq(KeyMap.decide(KeyMap.KEY_0 + 3, false, h), "HINT_DIGIT", "digit picks a hint"); eq(KeyMap.decide(KeyMap.STAR, false, h), "HINT_NEXT", "star = next page");
        eq(KeyMap.decide(KeyMap.SOFT_RIGHT, false, h), "HINT_EXIT", "cancel hints"); eq(KeyMap.decide(KeyMap.HOME, false, h), "HOME", "home still works");
        // type-ahead only in lists and never in apps that use digits
        eq(KeyMap.decide(KeyMap.KEY_0 + 5, false, cx().list(true).pkg("com.android.settings")), "TYPE_AHEAD", "digit in a list");
        eq(KeyMap.decide(KeyMap.KEY_0 + 5, false, cx().list(true).pkg("com.android.dialer")), "PASS", "dialer keeps its digits");
        eq(KeyMap.decide(KeyMap.KEY_0 + 5, false, cx().pkg("com.android.settings")), "PASS", "no list, no type-ahead");
        eq(KeyMap.decide(KeyMap.KEY_0 + 1, false, cx().list(true).pkg("x")), "PASS", "1 has no letters");
        eq(KeyMap.decide(KeyMap.KEY_0 + 5, false, cx().list(true).editing(true).pkg("x")), "PASS", "typing into a field");
    }

    static void typing() {
        MultiTap m = new MultiTap(); StringBuilder out = new StringBuilder(); long t = 0;
        int[][] word = {{4,2},{3,2},{5,3},{5,3},{6,3},{0,1},{9,1},{6,3},{7,3},{5,3},{3,1}};   // h e l l o, space, w o r l d  (key, taps)
        for (int[] kt : word) {
            for (int k = 0; k < kt[1]; k++) { MultiTap.Edit e = m.press(kt[0], t); if (e.deleteBefore > 0) out.setLength(out.length() - e.deleteBefore); out.append(e.insert); t += 200; }
            t += 1200; m.commitIfExpired(t);
        }
        eq(out, "Hello world", "hello world typed with auto capital");
        MultiTap c = new MultiTap(); c.cycleMode(); StringBuilder s = new StringBuilder(); long q = 0;
        for (int k = 0; k < 4; k++) { MultiTap.Edit e = c.press(2, q); if (e.deleteBefore > 0) s.setLength(s.length() - e.deleteBefore); s.append(e.insert); q += 100; }
        eq(s, "2", "four taps on 2 wrap to the digit");
        MultiTap d = new MultiTap(); d.cycleMode(); d.cycleMode(); d.cycleMode();
        eq(d.mode(), "NUM", "three # reach 123"); eq(d.press(7, 0).insert, "7", "number mode"); d.cycleMode(); eq(d.mode(), "PREDICT", "then predictive"); d.cycleMode(); eq(d.mode(), "CAP_NEXT", "then back to Abc");
        eq(MultiTap.label(MultiTap.Mode.PREDICT), "T9 predictive", "mode names"); eq(MultiTap.label(MultiTap.Mode.NUM), "123", "123"); eq(MultiTap.label(MultiTap.Mode.UPPER), "ABC", "ABC");
        MultiTap fm = new MultiTap(); fm.setMode(MultiTap.Mode.NUM); eq(fm.mode(), "NUM", "a number field forces 123");
        MultiTap tmo = new MultiTap(); tmo.cycleMode(); tmo.press(2, 0); eq(tmo.press(2, 2000).deleteBefore, 0, "slow tap = new letter");
        MultiTap up = new MultiTap(); eq(up.press(4, 0).insert, "G", "Abc mode capital"); up.commit(); eq(up.press(4, 2000).insert, "g", "then lower");
    }

    static void predictive() {
        List<String> dict = Arrays.asList("the\t222", "good\t160", "home\t150", "hello\t140", "gone\t120", "hood\t90", "he\t100", "hi\t150", "if\t200", "in\t210",
                "don't\t130", "i\t180", "hoof\t30", "homes\t20");
        T9Predictor p = new T9Predictor(dict);
        eq(T9Predictor.codeOf("good"), "4663", "code of good"); eq(T9Predictor.codeOf("don't"), "3668", "apostrophe skipped"); eq(T9Predictor.codeOf("a-b"), "null", "other characters rejected");
        eq(p.candidates("4663", 5), "[good, home, gone, hood, hoof]", "most frequent first");
        eq(p.candidates("43", 5), "[he, if, hello]", "he and if share 43, then a completion"); eq(p.candidates("3668", 3), "[don't]", "don't");
        eq(p.candidates("46637", 3), "[homes]", "no exact word, so a completion"); eq(p.candidates("", 3), "[]", "nothing typed"); eq(p.candidates("99999", 3), "[]", "nothing matches");
        T9Predictor.Word w = new T9Predictor.Word(p);
        w.start(false); eq(w.active(), false, "idle"); w.press(4); w.press(6); w.press(6); w.press(3);
        eq(w.text(), "good", "typing 4663 shows the best word"); w.nextCandidate(); eq(w.text(), "home", "* shows the next"); for (int k = 0; k < 5; k++) w.nextCandidate();      // six candidates in all: five words and the completion "homes"
        eq(w.text(), "good", "and wraps around");
        w.backspace(); eq(w.digits(), "466", "backspace removes the last key"); eq(w.text(), "good", "completion of 466");
        w.backspace(); w.backspace(); w.backspace(); eq(w.active(), false, "all keys removed"); eq(w.backspace(), false, "nothing to remove");
        w.start(true); w.press(4); w.press(3); eq(w.text(), "He", "capital at the start of a sentence");
        w.start(false); w.press(4); eq(w.text(), "I", "a lone i is shown as a capital I");
        w.start(false); w.press(4); w.press(4); eq(w.text(), "hi", "44 = hi");
        T9Predictor.Word x = new T9Predictor.Word(p); x.start(false); x.press(9); x.press(9); x.press(9);
        eq(x.hasMatch(), false, "unknown word"); eq(x.text(), "www", "unknown word shows the first letters");
        T9Predictor.Word iw = new T9Predictor.Word(p); iw.start(false); iw.press(4); eq(iw.candidates().contains("i"), true, "i is a candidate for 4"); 
    }

    static void hints() {
        // 12 items: two rows of 6 on screen, plus a stray one lower down
        List<R> items = new ArrayList<>();
        for (int i = 0; i < 6; i++) items.add(r(i * 40, 100, 30, 30));       // row 2 first in the list
        for (int i = 0; i < 6; i++) items.add(r(i * 40, 10, 30, 30));        // row 1
        HintModel hm = new HintModel(items);
        eq(hm.pageCount(), 2, "12 items = 2 pages"); eq(hm.page(0).size(), 10, "10 on page 1"); eq(hm.page(1).size(), 2, "2 on page 2");
        eq(hm.pick(0, 1), 6, "digit 1 = top-left item (reading order)"); eq(hm.pick(0, 6), 11, "digit 6 = end of the first row");
        eq(hm.pick(0, 0), hm.page(0).get(9), "digit 0 = tenth item"); eq(hm.pick(1, 3), -1, "empty slot"); eq(HintModel.digitOf(9), 0, "slot 10 shows a 0");
        eq(new HintModel(new ArrayList<R>()).pageCount(), 1, "no items still one page");
    }

    static void labels() {
        eq(SoftLabels.forState(false, false, false, "Wi-Fi", "Abc").center, "Wi-Fi", "item label in the centre");
        eq(SoftLabels.forState(false, false, false, null, "Abc").center, "Select", "default centre");
        eq(SoftLabels.forState(true, false, false, "x", "T9 predictive").center, "T9 predictive  (#)", "typing mode shown while editing");
        eq(SoftLabels.forState(false, true, false, "x", "Abc").center, "Tap", "pointer"); eq(SoftLabels.forState(false, false, true, "x", "Abc").right, "Cancel", "hints");
        eq(SoftLabels.forState(false, false, false, "a very long item label that will not fit", "Abc").center.length(), 18, "long labels are shortened");
    }

    static void pointer() {
        List<R> none = new ArrayList<>();
        int[] p = PointerModel.move(100, 100, Dir.RIGHT, 0, none, 240, 320); eq(p[0] + "," + p[1], "114,100", "one step");
        p = PointerModel.move(100, 100, Dir.DOWN, 20, none, 240, 320); eq(p[1], 190, "held for long moves fast");
        p = PointerModel.move(2, 2, Dir.LEFT, 0, none, 240, 320); eq(p[0], 0, "stops at the edge");
        List<R> one = Arrays.asList(r(120, 90, 60, 40));        // centre (150,110)
        p = PointerModel.move(100, 100, Dir.RIGHT, 0, one, 240, 320); eq(p[0] + "," + p[1], "114,100", "not yet inside");
        p = PointerModel.move(110, 100, Dir.RIGHT, 0, one, 240, 320); eq(p[0] + "," + p[1], "150,110", "snaps to the item centre");
        List<R> nested = Arrays.asList(r(100, 80, 140, 100), r(125, 95, 20, 20));
        p = PointerModel.move(115, 100, Dir.RIGHT, 0, nested, 240, 320); eq(p[0] + "," + p[1], "135,105", "the smaller item wins");
    }

    static void typeAhead() {
        List<String> l = Arrays.asList("Airplane mode", "Bluetooth", "Battery", "Display", "Wi-Fi", "Apps");
        eq(TypeAhead.next(l, -1, 2), 0, "2 = a,b,c from the start"); eq(TypeAhead.next(l, 0, 2), 1, "next a/b/c"); eq(TypeAhead.next(l, 2, 2), 5, "wraps and skips others");
        eq(TypeAhead.next(l, 5, 2), 0, "wraps around"); eq(TypeAhead.next(l, 0, 9), 4, "9 = w,x,y,z"); eq(TypeAhead.next(l, 0, 6), -1, "no m,n,o");
        eq(TypeAhead.next(l, 0, 1), -1, "1 has no letters"); eq(TypeAhead.next(new ArrayList<String>(), 0, 2), -1, "empty list");
    }

    static void slider() {
        SliderModel s = new SliderModel(1, 2);        // pretend key 1 = open sensor, key 2 = closed sensor
        eq(s.onKey(1, true), "OPEN", "open sensor pressed"); eq(s.onKey(1, true), "UNKNOWN", "no change reported twice");
        eq(s.onKey(2, true), "CLOSED", "closed sensor pressed"); eq(s.onKey(2, false), "OPEN", "closed sensor released = open");
        eq(s.onKey(9, true), "UNKNOWN", "other keys ignored");
        eq(SliderModel.decide(SliderModel.State.OPEN, true, false), "ANSWER_CALL", "open while ringing answers");
        eq(SliderModel.decide(SliderModel.State.OPEN, false, false), "WAKE", "open wakes");
        eq(SliderModel.decide(SliderModel.State.CLOSED, false, true), "END_CALL", "close ends the call");
        eq(SliderModel.decide(SliderModel.State.CLOSED, false, false), "SLEEP", "close locks");
        eq(SliderModel.decide(SliderModel.State.UNKNOWN, true, true), "NOTHING", "no change, no action");
    }

    static void labeler() {
        eq(Labeler.labelOf("  Wi-Fi\n ", null, "android.widget.Switch", true, true), "Wi-Fi, on", "text and state");
        eq(Labeler.labelOf(null, "Open menu", "android.widget.ImageButton", false, false), "Open menu", "description when no text");
        eq(Labeler.labelOf("", "", "android.widget.ImageButton", false, false), "button", "kind as the last resort");
        eq(Labeler.labelOf(null, null, "android.widget.EditText", false, false), "text field", "text field");
    }
}
