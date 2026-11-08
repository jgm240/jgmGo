package org.jgmgo.keys;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lets the whole phone be used with the keypad (see device/README.md for the key list). Runs as an accessibility service,
 * so it works in every app without the app's help: arrows move a highlight, OK presses, long-press # shows number
 * hints, long-press * gives a free pointer that snaps to buttons, a bar above the soft keys says what they do.
 */
public class KeyNavService extends AccessibilityService {
    private static final long LONG_PRESS_MS = 600;
    private static final int KEY_F11 = KeyEvent.KEYCODE_F11, KEY_F12 = KeyEvent.KEYCODE_F12;
    private final Handler handler = new Handler();
    private Overlay overlay;
    private TextToSpeech tts; private boolean ttsReady;
    private SliderModel slider;

    private boolean pointer = false, hints = false;
    private int px, py, pointerRepeat, hintPage;
    private Rect selected = null;
    private String selectedLabel = "";
    private HintModel hintModel;
    private List<Rect> hintRects = new ArrayList<>();
    private int heldKey = -1;
    private boolean longFired = false;
    private final Runnable longPress = new Runnable() { @Override public void run() { longFired = true; act(heldKey, true); } };

    @Override protected void onServiceConnected() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        overlay = new Overlay(this);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        wm.addView(overlay, lp);
        px = getResources().getDisplayMetrics().widthPixels / 2;
        py = getResources().getDisplayMetrics().heightPixels / 2;
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override public void onInit(int status) { ttsReady = status == TextToSpeech.SUCCESS; if (ttsReady) tts.setLanguage(Locale.getDefault()); }
        });
        int openKey = Hidden.getIntProp("persist.jgm.slider_open_key", KEY_F11);
        slider = new SliderModel(openKey, openKey == KEY_F11 ? KEY_F12 : KEY_F11);
    }

    @Override public void onDestroy() { if (tts != null) tts.shutdown(); super.onDestroy(); }
    @Override public void onAccessibilityEvent(AccessibilityEvent e) { if (overlay != null) overlay.invalidate(); }
    @Override public void onInterrupt() {}

    // ---- keys ------------------------------------------------------------------------------------------------------
    @Override protected boolean onKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KEY_F11 || code == KEY_F12) { sliderKey(code, e.getAction() == KeyEvent.ACTION_DOWN); return true; }
        KeyMap.Ctx ctx = context();
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            if (e.getRepeatCount() == 0) {
                heldKey = code; longFired = false;
                if (KeyMap.decide(code, false, ctx) == KeyMap.Action.PASS && KeyMap.decide(code, true, ctx) == KeyMap.Action.PASS) return false;
                if (isArrow(code) || ctx.hints || KeyMap.decide(code, false, ctx) == KeyMap.Action.TYPE_AHEAD) { pointerRepeat = 0; act(code, false); return true; }
                handler.postDelayed(longPress, LONG_PRESS_MS);
                return true;
            }
            if (isArrow(code)) { pointerRepeat = e.getRepeatCount(); act(code, false); return true; }
            return KeyMap.decide(code, false, ctx) != KeyMap.Action.PASS;
        }
        if (e.getAction() == KeyEvent.ACTION_UP) {
            if (KeyMap.decide(code, false, ctx) == KeyMap.Action.PASS && KeyMap.decide(code, true, ctx) == KeyMap.Action.PASS) return false;
            if (code != heldKey) return true;
            handler.removeCallbacks(longPress);
            if (!longFired && !isArrow(code) && !ctx.hints && KeyMap.decide(code, false, ctx) != KeyMap.Action.TYPE_AHEAD) act(code, false);
            heldKey = -1;
            return true;
        }
        return false;
    }
    private static boolean isArrow(int c) { return c == KeyEvent.KEYCODE_DPAD_UP || c == KeyEvent.KEYCODE_DPAD_DOWN || c == KeyEvent.KEYCODE_DPAD_LEFT || c == KeyEvent.KEYCODE_DPAD_RIGHT; }

    private KeyMap.Ctx context() {
        KeyMap.Ctx c = new KeyMap.Ctx();
        c.pointer = pointer; c.hints = hints; c.editing = isEditing();
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null && root.getPackageName() != null) c.pkg = root.getPackageName().toString();
        c.list = !pointer && !c.editing && selected != null && inList(root);
        return c;
    }

    private void act(int code, boolean longPress) {
        KeyMap.Ctx ctx = context();
        KeyMap.Action a = KeyMap.decide(code, longPress, ctx);
        if (hints && a != KeyMap.Action.HINT_DIGIT && a != KeyMap.Action.HINT_NEXT && a != KeyMap.Action.PASS) { if (a != KeyMap.Action.HINT_EXIT) setHints(false); }
        switch (a) {
            case BACK: performGlobalAction(GLOBAL_ACTION_BACK); break;
            case HOME: performGlobalAction(GLOBAL_ACTION_HOME); break;
            case RECENTS: performGlobalAction(GLOBAL_ACTION_RECENTS); break;
            case NOTIFICATIONS: performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS); break;
            case QUICK_SETTINGS: performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS); break;
            case POINTER_TOGGLE: setPointer(!pointer); break;
            case HINTS_TOGGLE: setHints(!hints); break;
            case HINT_EXIT: setHints(false); break;
            case HINT_NEXT: hintPage = (hintPage + 1) % hintModel.pageCount(); overlay.invalidate(); break;
            case HINT_DIGIT: pickHint(KeyMap.digitOf(code)); break;
            case TYPE_AHEAD: typeAhead(KeyMap.digitOf(code)); break;
            case NAV_UP: move(NavModel.Dir.UP); break;
            case NAV_DOWN: move(NavModel.Dir.DOWN); break;
            case NAV_LEFT: move(NavModel.Dir.LEFT); break;
            case NAV_RIGHT: move(NavModel.Dir.RIGHT); break;
            case CLICK: press(false); break;
            case LONG_CLICK: press(true); break;
            default: break;
        }
    }

    // ---- modes -----------------------------------------------------------------------------------------------------
    private void setPointer(boolean on) {
        pointer = on; if (on) hints = false;
        Toast.makeText(this, on ? "Pointer: arrows move it, OK taps, long * to leave" : "Highlight mode", Toast.LENGTH_SHORT).show();
        overlay.invalidate();
    }

    private void setHints(boolean on) {
        if (!on) { hints = false; overlay.invalidate(); return; }
        List<AccessibilityNodeInfo> nodes = new ArrayList<>(); collect(getRootInActiveWindow(), nodes);
        if (nodes.isEmpty()) { Toast.makeText(this, "Nothing to pick on this screen", Toast.LENGTH_SHORT).show(); return; }
        hintRects.clear(); List<NavModel.R> rs = new ArrayList<>();
        for (AccessibilityNodeInfo n : nodes) { Rect b = new Rect(); n.getBoundsInScreen(b); hintRects.add(b); rs.add(new NavModel.R(b.left, b.top, b.right, b.bottom)); }
        hintModel = new HintModel(rs); hintPage = 0; hints = true; pointer = false;
        overlay.invalidate();
    }

    private void pickHint(int digit) {
        int idx = hintModel == null ? -1 : hintModel.pick(hintPage, digit);
        setHints(false);
        if (idx < 0 || idx >= hintRects.size()) return;
        selected = hintRects.get(idx);
        press(false);
    }

    // ---- moving ----------------------------------------------------------------------------------------------------
    private void move(NavModel.Dir d) {
        if (pointer) { movePointer(d); return; }
        List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        collect(getRootInActiveWindow(), nodes);
        if (nodes.isEmpty()) { setPointer(true); movePointer(d); return; }      // nothing to select here: use the free pointer
        List<NavModel.R> rects = rectsOf(nodes);
        int cur = indexOf(rects, selected);
        int next = NavModel.next(rects, cur, d);
        if (next >= 0) { select(nodes.get(next)); return; }
        boolean forward = d == NavModel.Dir.DOWN || d == NavModel.Dir.RIGHT;
        AccessibilityNodeInfo sc = findScrollable(getRootInActiveWindow());
        if (sc != null) sc.performAction(forward ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
    }

    private void select(AccessibilityNodeInfo n) {
        Rect b = new Rect(); n.getBoundsInScreen(b); selected = b;
        selectedLabel = Labeler.labelOf(n.getText(), n.getContentDescription(), String.valueOf(n.getClassName()), n.isCheckable(), n.isChecked());
        n.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
        n.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.getId());      // scroll it into view
        if (ttsReady && Hidden.getBoolProp("persist.jgm.speak", false)) tts.speak(selectedLabel, TextToSpeech.QUEUE_FLUSH, null, "jgmkeys");
        overlay.invalidate();
    }

    private void typeAhead(int digit) {
        List<AccessibilityNodeInfo> nodes = new ArrayList<>(); collect(getRootInActiveWindow(), nodes);
        List<String> labels = new ArrayList<>();
        for (AccessibilityNodeInfo n : nodes) labels.add(Labeler.labelOf(n.getText(), n.getContentDescription(), String.valueOf(n.getClassName()), false, false));
        int idx = TypeAhead.next(labels, indexOf(rectsOf(nodes), selected), digit);
        if (idx >= 0) select(nodes.get(idx));
    }

    private void movePointer(NavModel.Dir d) {
        List<AccessibilityNodeInfo> nodes = new ArrayList<>(); collect(getRootInActiveWindow(), nodes);
        int w = getResources().getDisplayMetrics().widthPixels, h = getResources().getDisplayMetrics().heightPixels;
        List<NavModel.R> items = rectsOf(nodes);
        int[] p = PointerModel.move(px, py, d, pointerRepeat, items, w, h);
        px = p[0]; py = p[1];
        overlay.invalidate();
    }

    // ---- finding things --------------------------------------------------------------------------------------------
    private List<NavModel.R> rectsOf(List<AccessibilityNodeInfo> nodes) {
        List<NavModel.R> rects = new ArrayList<>();
        for (AccessibilityNodeInfo n : nodes) { Rect b = new Rect(); n.getBoundsInScreen(b); rects.add(new NavModel.R(b.left, b.top, b.right, b.bottom)); }
        return rects;
    }
    private int indexOf(List<NavModel.R> rects, Rect s) {
        if (s == null) return -1;
        int best = -1; long bd = Long.MAX_VALUE;
        for (int i = 0; i < rects.size(); i++) {
            NavModel.R r = rects.get(i);
            if (r.l == s.left && r.t == s.top && r.r == s.right && r.b == s.bottom) return i;
            long dx = (r.l + r.r) / 2 - s.centerX(), dy = (r.t + r.b) / 2 - s.centerY(), dist = dx * dx + dy * dy;
            if (dist < bd) { bd = dist; best = i; }
        }
        return best;
    }
    private void collect(AccessibilityNodeInfo n, List<AccessibilityNodeInfo> out) {
        if (n == null) return;
        if (n.isVisibleToUser() && n.isEnabled() && (n.isClickable() || n.isEditable() || n.isCheckable())) {
            Rect b = new Rect(); n.getBoundsInScreen(b);
            if (b.width() > 2 && b.height() > 2) out.add(n);
        }
        for (int i = 0; i < n.getChildCount(); i++) collect(n.getChild(i), out);
    }
    private AccessibilityNodeInfo findScrollable(AccessibilityNodeInfo n) {
        if (n == null) return null;
        if (n.isScrollable() && n.isVisibleToUser()) return n;
        for (int i = 0; i < n.getChildCount(); i++) { AccessibilityNodeInfo r = findScrollable(n.getChild(i)); if (r != null) return r; }
        return null;
    }
    private boolean inList(AccessibilityNodeInfo root) {
        if (root == null) return false;
        List<AccessibilityNodeInfo> nodes = new ArrayList<>(); collect(root, nodes);
        return nodes.size() >= 6 && findScrollable(root) != null;
    }
    private boolean isEditing() { AccessibilityNodeInfo f = findFocus(AccessibilityNodeInfo.FOCUS_INPUT); return f != null && f.isEditable(); }

    // ---- pressing --------------------------------------------------------------------------------------------------
    private void press(boolean longPress) {
        if (pointer) { tap(px, py, longPress); return; }
        if (selected == null) return;
        AccessibilityNodeInfo hit = findByBounds(getRootInActiveWindow(), selected);
        if (hit != null && hit.performAction(longPress ? AccessibilityNodeInfo.ACTION_LONG_CLICK : AccessibilityNodeInfo.ACTION_CLICK)) return;
        tap(selected.centerX(), selected.centerY(), longPress);
    }
    private AccessibilityNodeInfo findByBounds(AccessibilityNodeInfo n, Rect b) {
        if (n == null) return null;
        Rect r = new Rect(); n.getBoundsInScreen(r);
        if (r.equals(b) && (n.isClickable() || n.isEditable() || n.isCheckable())) return n;
        for (int i = 0; i < n.getChildCount(); i++) { AccessibilityNodeInfo f = findByBounds(n.getChild(i), b); if (f != null) return f; }
        return null;
    }
    private void tap(int x, int y, boolean longPress) {
        Path p = new Path(); p.moveTo(x, y);
        dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(p, 0, longPress ? 700 : 60)).build(), null, null);
    }

    // ---- the slider ------------------------------------------------------------------------------------------------
    private void sliderKey(int code, boolean down) {
        SliderModel.State s = slider.onKey(code, down);
        switch (SliderModel.decide(s, Hidden.ringing(this), Hidden.inCall(this))) {
            case ANSWER_CALL: Hidden.answerCall(this); break;
            case WAKE: Hidden.wakeNow(this); break;
            case END_CALL: Hidden.endCall(this); break;
            case SLEEP: Hidden.sleepNow(this); break;
            default: break;
        }
    }

    // ---- what is drawn on top of everything ------------------------------------------------------------------------
    private final class Overlay extends View {
        private final Paint box = new Paint(Paint.ANTI_ALIAS_FLAG), dot = new Paint(Paint.ANTI_ALIAS_FLAG), bar = new Paint(), txt = new Paint(Paint.ANTI_ALIAS_FLAG), badge = new Paint(Paint.ANTI_ALIAS_FLAG);
        Overlay(Context c) {
            super(c);
            box.setStyle(Paint.Style.STROKE); box.setStrokeWidth(4); box.setColor(0xFF2DE0A5);
            dot.setStyle(Paint.Style.FILL); dot.setColor(0xFFFFFFFF);
            bar.setColor(0xE6081218); txt.setColor(0xFFF5F7FA); txt.setTextSize(13 * getResources().getDisplayMetrics().density);
            badge.setColor(0xFF2DE0A5);
        }
        @Override protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight(); float d = getResources().getDisplayMetrics().density;
            if (hints && hintModel != null) {
                List<Integer> page = hintModel.page(hintPage);
                for (int i = 0; i < page.size(); i++) {
                    Rect r = hintRects.get(page.get(i));
                    c.drawRoundRect(r.left, r.top, r.right, r.bottom, 6, 6, box);
                    float bw = 16 * d, bh = 16 * d;
                    c.drawRoundRect(r.left, r.top, r.left + bw, r.top + bh, 4, 4, badge);
                    txt.setColor(0xFF081218); c.drawText(String.valueOf(HintModel.digitOf(i)), r.left + 4 * d, r.top + bh - 3 * d, txt); txt.setColor(0xFFF5F7FA);
                }
            } else if (pointer) {
                c.drawCircle(px, py, 9, dot); c.drawCircle(px, py, 9, box);
            } else if (selected != null) {
                c.drawRoundRect(selected.left, selected.top, selected.right, selected.bottom, 6, 6, box);
            }
            // the bar above the soft keys: what left, centre and right do right now
            SoftLabels l = SoftLabels.forState(isEditing(), pointer, hints, selectedLabel, ModeState.typingMode);
            float bh = 20 * d; c.drawRect(0, h - bh, w, h, bar);
            c.drawText(l.left, 4 * d, h - 5 * d, txt);
            c.drawText(l.right, w - txt.measureText(l.right) - 4 * d, h - 5 * d, txt);
            String center = l.center; float cw = txt.measureText(center), maxw = w - 2 * 60 * d;
            while (cw > maxw && center.length() > 2) { center = center.substring(0, center.length() - 2) + "\u2026"; cw = txt.measureText(center); }
            c.drawText(center, (w - cw) / 2, h - 5 * d, txt);
        }
    }
}
