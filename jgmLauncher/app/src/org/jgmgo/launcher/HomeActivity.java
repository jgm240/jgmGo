package org.jgmgo.launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import org.jgmgo.keys.TypeAhead;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Home screen for a keypad phone. The number keys are the launcher: 2..9 open the app on that key (hold to choose another),
 * 1 = voicemail, hold 0 = flashlight, 0 = all apps (number keys jump by first letter, OK opens, hold OK puts it on a key).
 */
public class HomeActivity extends Activity {
    private static final String[] PREFERRED = {"dialer", "messaging", "contacts", "camera", "settings", "deskclock", "calculator", "browser"};
    private SharedPreferences prefs;
    private Favorites fav;
    private final List<App> apps = new ArrayList<>();
    private GridLayout grid;
    private boolean drawer = false;
    private int drawerSel = 0;
    private LinearLayout root;
    private boolean torch = false;
    private int assigning = -1;               // a number key being assigned from the drawer

    private static final class App { String label; ComponentName cn; Drawable icon; String id() { return cn.flattenToString(); } }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("home", MODE_PRIVATE);
        loadApps();
        fav = Favorites.parse(prefs.getString("fav", ""));
        List<String> ids = new ArrayList<>(); for (App a : apps) ids.add(a.id());
        fav.fillDefaults(ids, PREFERRED); save();
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(0xFF0C1218);
        setContentView(root);
        showHome();
    }
    @Override protected void onResume() { super.onResume(); if (!drawer) showHome(); }
    @Override public void onBackPressed() { if (drawer) { assigning = -1; showHome(); } }       // on the home screen back does nothing

    private void save() { prefs.edit().putString("fav", fav.serialize()).apply(); }

    private void loadApps() {
        apps.clear();
        PackageManager pm = getPackageManager();
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(i, 0)) {
            if (r.activityInfo.packageName.equals(getPackageName())) continue;
            App a = new App(); a.label = String.valueOf(r.loadLabel(pm)); a.icon = r.loadIcon(pm);
            a.cn = new ComponentName(r.activityInfo.packageName, r.activityInfo.name); apps.add(a);
        }
        Collections.sort(apps, new Comparator<App>() { @Override public int compare(App x, App y) { return x.label.compareToIgnoreCase(y.label); } });
    }
    private App find(String id) { for (App a : apps) if (a.id().equals(id)) return a; return null; }

    // ---- home screen: a 3x4 grid laid out like the keypad ------------------------------------------------------------
    private void showHome() {
        drawer = false; root.removeAllViews();
        TextClock clock = new TextClock(this); clock.setFormat12Hour("h:mm"); clock.setFormat24Hour("HH:mm");
        clock.setTextSize(30); clock.setTextColor(0xFFF5F7FA); clock.setGravity(Gravity.CENTER);
        root.addView(clock, new LinearLayout.LayoutParams(-1, -2));
        grid = new GridLayout(this); grid.setColumnCount(3); grid.setRowCount(4);
        int[] order = {1, 2, 3, 4, 5, 6, 7, 8, 9, -1, 0, -1};
        for (int d : order) {
            LinearLayout cell = new LinearLayout(this); cell.setOrientation(LinearLayout.VERTICAL); cell.setGravity(Gravity.CENTER);
            if (d >= 0) {
                String label; Drawable icon = null;
                if (d == 1) label = "Voicemail"; else if (d == 0) label = "All apps";
                else { App a = fav.get(d) == null ? null : find(fav.get(d)); label = a == null ? "(empty)" : a.label; icon = a == null ? null : a.icon; }
                if (icon != null) { ImageView iv = new ImageView(this); iv.setImageDrawable(icon); cell.addView(iv, new LinearLayout.LayoutParams(40, 40)); }
                TextView num = new TextView(this); num.setText(String.valueOf(d)); num.setTextColor(0xFF2DE0A5); num.setTextSize(13); cell.addView(num);
                TextView t = new TextView(this); t.setText(label.length() > 9 ? label.substring(0, 8) + "…" : label); t.setTextColor(0xFFF5F7FA); t.setTextSize(10); cell.addView(t);
            }
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(); lp.width = 78; lp.height = 62; cell.setLayoutParams(lp);
            grid.addView(cell);
        }
        root.addView(grid, new LinearLayout.LayoutParams(-1, -2));
    }

    // ---- all apps -------------------------------------------------------------------------------------------------------
    private void showDrawer() {
        drawer = true; root.removeAllViews(); drawerSel = Math.min(drawerSel, Math.max(0, apps.size() - 1));
        TextView h = new TextView(this); h.setTextColor(0xFF2DE0A5); h.setTextSize(13);
        h.setText(assigning >= 0 ? "Choose app for key " + assigning + " (OK)" : "All apps   (2-9 jump by letter, hold OK = put on a key)"); root.addView(h);
        int from = Math.max(0, drawerSel - 5), to = Math.min(apps.size(), from + 11);
        for (int i = from; i < to; i++) {
            TextView t = new TextView(this); t.setText(apps.get(i).label); t.setTextSize(15); t.setPadding(8, 5, 8, 5);
            t.setTextColor(0xFFF5F7FA); t.setBackgroundColor(i == drawerSel ? 0xFF1D3A33 : 0x00000000); root.addView(t);
        }
    }
    private List<String> labels() { List<String> l = new ArrayList<>(); for (App a : apps) l.add(a.label); return l; }

    // ---- keys -----------------------------------------------------------------------------------------------------------
    @Override public boolean onKeyDown(int code, KeyEvent e) {
        boolean digit = code >= KeyEvent.KEYCODE_0 && code <= KeyEvent.KEYCODE_9;
        if (drawer) {
            if (code == KeyEvent.KEYCODE_DPAD_DOWN) { drawerSel = Math.min(apps.size() - 1, drawerSel + 1); showDrawer(); return true; }
            if (code == KeyEvent.KEYCODE_DPAD_UP) { drawerSel = Math.max(0, drawerSel - 1); showDrawer(); return true; }
            if (digit) { int i = TypeAhead.next(labels(), drawerSel, code - KeyEvent.KEYCODE_0); if (i >= 0) { drawerSel = i; showDrawer(); } return true; }
            if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) {
                e.startTracking();
                return true;
            }
            return super.onKeyDown(code, e);
        }
        if (digit) { e.startTracking(); return true; }
        return super.onKeyDown(code, e);
    }

    @Override public boolean onKeyUp(int code, KeyEvent e) {
        boolean digit = code >= KeyEvent.KEYCODE_0 && code <= KeyEvent.KEYCODE_9;
        if ((e.getFlags() & KeyEvent.FLAG_CANCELED_LONG_PRESS) != 0) return true;       // the long press already did its job
        if (drawer && (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) && !apps.isEmpty()) {
            if (assigning >= 0) { fav.set(assigning, apps.get(drawerSel).id()); save(); assigning = -1; showHome(); }
            else launch(apps.get(drawerSel).cn);
            return true;
        }
        if (!drawer && digit) {
            int d = code - KeyEvent.KEYCODE_0;
            if (d == 0) { showDrawer(); return true; }
            if (d == 1) { voicemail(); return true; }
            String id = fav.get(d);
            if (id == null) { assigning = d; showDrawer(); } else { App a = find(id); if (a != null) launch(a.cn); }
            return true;
        }
        return super.onKeyUp(code, e);
    }

    @Override public boolean onKeyLongPress(int code, KeyEvent e) {
        boolean digit = code >= KeyEvent.KEYCODE_0 && code <= KeyEvent.KEYCODE_9;
        if (!drawer && digit) {
            int d = code - KeyEvent.KEYCODE_0;
            if (d == 0) { flashlight(); return true; }
            if (Favorites.assignable(d)) { assigning = d; showDrawer(); return true; }
        }
        if (drawer && (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) && !apps.isEmpty()) {
            Toast.makeText(this, "Press a number key 2-9", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onKeyLongPress(code, e);
    }

    private void launch(ComponentName cn) {
        try { startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(cn).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (Exception ex) { Toast.makeText(this, "Cannot open it", Toast.LENGTH_SHORT).show(); }
    }
    private void voicemail() {
        try {
            TelecomManager tm = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
            String num = tm.getVoiceMailNumber(null);
            if (num == null) { Toast.makeText(this, "No voicemail number", Toast.LENGTH_SHORT).show(); return; }
            startActivity(new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", num, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception ex) { Toast.makeText(this, "Cannot call voicemail", Toast.LENGTH_SHORT).show(); }
    }
    private void flashlight() {
        try {
            CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            torch = !torch; cm.setTorchMode(cm.getCameraIdList()[0], torch);
            Toast.makeText(this, torch ? "Flashlight on" : "Flashlight off", Toast.LENGTH_SHORT).show();
        } catch (Exception ex) { Toast.makeText(this, "No flashlight", Toast.LENGTH_SHORT).show(); }
    }
}
