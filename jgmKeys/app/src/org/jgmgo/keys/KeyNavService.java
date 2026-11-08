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
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets the whole phone be used with the keypad. Arrow keys move a highlight between the clickable things on screen, OK
 * presses, soft keys do back/recents/notifications. Long-press * switches to a free pointer for screens with nothing to
 * select (games, maps). Runs as an accessibility service, so it works in every app without the app's help.
 */
public class KeyNavService extends AccessibilityService {
    private static final long LONG_PRESS_MS = 600;
    private final Handler handler = new Handler();
    private Overlay overlay;
    private boolean pointer = false;
    private int px, py, pointerStep = 16;
    private Rect selected = null;                 // bounds of the highlighted item
    private int heldKey = -1;
    private boolean longFired = false;
    private final Runnable longPress = new Runnable() {
        @Override public void run() { longFired = true; run(heldKey, true); }
    };

    @Override protected void onServiceConnected() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        overlay = new Overlay(this);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        wm.addView(overlay, lp);
        px = getResources().getDisplayMetrics().widthPixels / 2;
        py = getResources().getDisplayMetrics().heightPixels / 2;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        // The screen changed: keep the highlight on something that still exists.
        if (!pointer && selected != null) { overlay.invalidate(); }
    }
    @Override public void onInterrupt() {}

    @Override protected boolean onKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        boolean editing = isEditing();
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            if (e.getRepeatCount() == 0) {
                heldKey = code; longFired = false;
                KeyMap.Action probe = KeyMap.decide(code, false, editing, pointer);
                if (probe == KeyMap.Action.PASS) return false;
                // Only some keys have a long-press meaning; the arrows act immediately and repeat while held.
                if (isArrow(code)) { run(code, false); return true; }
                handler.postDelayed(longPress, LONG_PRESS_MS);
                return true;
            }
            if (isArrow(code)) { run(code, false); return true; }       // key repeat while held
            return KeyMap.decide(code, false, editing, pointer) != KeyMap.Action.PASS;
        }
        if (e.getAction() == KeyEvent.ACTION_UP) {
            if (KeyMap.decide(code, false, editing, pointer) == KeyMap.Action.PASS) return false;
            if (code != heldKey) return true;
            handler.removeCallbacks(longPress);
            if (!longFired && !isArrow(code)) run(code, false);
            heldKey = -1;
            return true;
        }
        return false;
    }

    private static boolean isArrow(int c) {
        return c == KeyEvent.KEYCODE_DPAD_UP || c == KeyEvent.KEYCODE_DPAD_DOWN || c == KeyEvent.KEYCODE_DPAD_LEFT || c == KeyEvent.KEYCODE_DPAD_RIGHT;
    }

    private void run(int code, boolean longPress) {
        KeyMap.Action a = KeyMap.decide(code, longPress, isEditing(), pointer);
        switch (a) {
            case BACK: performGlobalAction(GLOBAL_ACTION_BACK); break;
            case HOME: performGlobalAction(GLOBAL_ACTION_HOME); break;
            case RECENTS: performGlobalAction(GLOBAL_ACTION_RECENTS); break;
            case NOTIFICATIONS: performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS); break;
            case QUICK_SETTINGS: performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS); break;
            case POINTER_TOGGLE: setPointer(!pointer); break;
            case NAV_UP: move(NavModel.Dir.UP); break;
            case NAV_DOWN: move(NavModel.Dir.DOWN); break;
            case NAV_LEFT: move(NavModel.Dir.LEFT); break;
            case NAV_RIGHT: move(NavModel.Dir.RIGHT); break;
            case CLICK: press(false); break;
            case LONG_CLICK: press(true); break;
            default: break;
        }
    }

    private void setPointer(boolean on) {
        pointer = on;
        Toast.makeText(this, on ? "Pointer: arrows move it, OK taps" : "Highlight mode", Toast.LENGTH_SHORT).show();
        overlay.invalidate();
    }

    // ---- moving ----------------------------------------------------------------------------------------------------
    private void move(NavModel.Dir d) {
        if (pointer) { movePointer(d); return; }
        List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        collect(getRootInActiveWindow(), nodes);
        if (nodes.isEmpty()) {          // nothing to select on this screen: fall back to the free pointer
            setPointer(true); movePointer(d); return;
        }
        List<NavModel.R> rects = new ArrayList<>();
        int cur = -1;
        for (int i = 0; i < nodes.size(); i++) {
            Rect b = new Rect(); nodes.get(i).getBoundsInScreen(b);
            rects.add(new NavModel.R(b.left, b.top, b.right, b.bottom));
            if (selected != null && selected.equals(b)) cur = i;
        }
        if (cur < 0 && selected != null) cur = closest(rects, selected);
        int next = NavModel.next(rects, cur, d);
        if (next >= 0) {
            AccessibilityNodeInfo n = nodes.get(next);
            Rect b = new Rect(); n.getBoundsInScreen(b);
            selected = b;
            n.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            overlay.invalidate();
            return;
        }
        // End of the list in this direction: scroll the screen instead.
        boolean forward = d == NavModel.Dir.DOWN || d == NavModel.Dir.RIGHT;
        AccessibilityNodeInfo sc = findScrollable(getRootInActiveWindow());
        if (sc != null) sc.performAction(forward ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
    }

    private int closest(List<NavModel.R> rects, Rect s) {
        int best = -1; long bd = Long.MAX_VALUE;
        for (int i = 0; i < rects.size(); i++) {
            NavModel.R r = rects.get(i);
            long dx = (r.l + r.r) / 2 - s.centerX(), dy = (r.t + r.b) / 2 - s.centerY();
            long dist = dx * dx + dy * dy;
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

    private boolean isEditing() {
        AccessibilityNodeInfo f = findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        return f != null && f.isEditable();
    }

    // ---- pointer ---------------------------------------------------------------------------------------------------
    private void movePointer(NavModel.Dir d) {
        int w = getResources().getDisplayMetrics().widthPixels, h = getResources().getDisplayMetrics().heightPixels;
        switch (d) { case UP: py -= pointerStep; break; case DOWN: py += pointerStep; break; case LEFT: px -= pointerStep; break; default: px += pointerStep; break; }
        px = Math.max(0, Math.min(w - 1, px)); py = Math.max(0, Math.min(h - 1, py));
        overlay.invalidate();
    }

    // ---- pressing --------------------------------------------------------------------------------------------------
    private void press(boolean longPress) {
        if (pointer) { tap(px, py, longPress); return; }
        if (selected == null) return;
        AccessibilityNodeInfo hit = findByBounds(getRootInActiveWindow(), selected);
        if (hit != null && hit.performAction(longPress ? AccessibilityNodeInfo.ACTION_LONG_CLICK : AccessibilityNodeInfo.ACTION_CLICK)) return;
        tap(selected.centerX(), selected.centerY(), longPress);      // the app did not accept a click action: touch the spot instead
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
        GestureDescription.StrokeDescription s = new GestureDescription.StrokeDescription(p, 0, longPress ? 700 : 60);
        dispatchGesture(new GestureDescription.Builder().addStroke(s).build(), null, null);
    }

    // ---- what is drawn on top of everything ------------------------------------------------------------------------
    private final class Overlay extends View {
        private final Paint box = new Paint(Paint.ANTI_ALIAS_FLAG), dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        Overlay(Context c) {
            super(c);
            box.setStyle(Paint.Style.STROKE); box.setStrokeWidth(4); box.setColor(0xFF2DE0A5);
            dot.setStyle(Paint.Style.FILL); dot.setColor(0xFFFFFFFF);
        }
        @Override protected void onDraw(Canvas c) {
            if (pointer) {
                c.drawCircle(px, py, 9, dot);
                box.setColor(0xFF2DE0A5); c.drawCircle(px, py, 9, box);
            } else if (selected != null) {
                c.drawRoundRect(selected.left, selected.top, selected.right, selected.bottom, 6, 6, box);
            }
        }
    }
}
