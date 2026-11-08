package org.jgmdroid.tweaks;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;

/** Keypad-first settings screen: up/down to pick a row, left/right to change it, OK on "Apply". Needs no touchscreen. */
public class MainActivity extends Activity {
    private static final int ROW_ZRAM = 0, ROW_SWAPPINESS = 1, ROW_SWAPFILE = 2, ROW_SPEAK = 3, ROW_APPLY = 4, ROWS = 5;
    private final TextView[] rows = new TextView[ROWS];
    private TextView info, status;
    private int selected = 0;
    private int zram, swappiness, swapfile;
    private boolean speak;
    private final Handler handler = new Handler();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        zram = Tweaks.snap(Props.getInt(Tweaks.PROP_ZRAM, Tweaks.DEFAULT_ZRAM), Tweaks.ZRAM_MB);
        swappiness = Tweaks.clampSwappiness(Props.getInt(Tweaks.PROP_SWAPPINESS, Tweaks.DEFAULT_SWAPPINESS));
        swapfile = Tweaks.snap(Props.getInt(Tweaks.PROP_SWAPFILE, Tweaks.DEFAULT_SWAPFILE), Tweaks.SWAPFILE_MB);

        speak = Props.getBool("persist.jgm.speak", false);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0C1218);
        root.setPadding(10, 8, 10, 8);
        TextView title = text("jgmTweaks", 18, 0xFFF5F7FA);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);
        info = text("", 11, 0xFF9AAFA8);
        root.addView(info);
        for (int i = 0; i < ROWS; i++) {
            rows[i] = text("", 15, 0xFFF5F7FA);
            rows[i].setPadding(8, 8, 8, 8);
            root.addView(rows[i]);
        }
        status = text("", 11, 0xFF2DE0A5);
        root.addView(status);
        TextView hint = text("Up/Down: row   Left/Right: change   OK: apply", 10, 0xFF6F8580);
        root.addView(hint);
        setContentView(root);
        refresh();
        tick.run();
    }

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        return t;
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() { info.setText(memorySummary()); handler.postDelayed(this, 2000); }
    };
    @Override protected void onDestroy() { handler.removeCallbacks(tick); super.onDestroy(); }

    private static String memorySummary() {
        long total = 0, free = 0, cached = 0, swapTotal = 0, swapFree = 0;
        try (BufferedReader r = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String l;
            while ((l = r.readLine()) != null) {
                String[] p = l.split("\\s+");
                if (p.length < 2) continue;
                long kb = Long.parseLong(p[1]);
                switch (p[0]) {
                    case "MemTotal:": total = kb; break;
                    case "MemFree:": free = kb; break;
                    case "Cached:": cached = kb; break;
                    case "SwapTotal:": swapTotal = kb; break;
                    case "SwapFree:": swapFree = kb; break;
                    default: break;
                }
            }
        } catch (Exception e) { return "memory info unavailable"; }
        return "RAM " + (total / 1024) + " MB, free " + ((free + cached) / 1024) + " MB\nSwap " + ((swapTotal - swapFree) / 1024) + " / " + (swapTotal / 1024) + " MB used";
    }

    private void refresh() {
        rows[ROW_ZRAM].setText("zram:  < " + Tweaks.label(zram) + " >");
        rows[ROW_SWAPPINESS].setText("Swappiness:  < " + swappiness + " >");
        rows[ROW_SWAPFILE].setText("Swap file:  < " + Tweaks.label(swapfile) + " >");
        rows[ROW_SPEAK].setText("Speak items:  < " + (speak ? "On" : "Off") + " >");
        rows[ROW_APPLY].setText("[ Apply ]");
        for (int i = 0; i < ROWS; i++) rows[i].setBackgroundColor(i == selected ? 0xFF1D3A33 : 0x00000000);
    }

    private void change(int dir) {
        switch (selected) {
            case ROW_ZRAM: zram = Tweaks.step(Tweaks.ZRAM_MB, zram, dir); break;
            case ROW_SWAPPINESS: swappiness = Tweaks.clampSwappiness(swappiness + dir * Tweaks.SWAPPINESS_STEP); break;
            case ROW_SWAPFILE: swapfile = Tweaks.step(Tweaks.SWAPFILE_MB, swapfile, dir); break;
            case ROW_SPEAK: speak = !speak; Props.set(Tweaks.PROP_SPEAK, speak ? "true" : "false"); break;          // takes effect at once
            default: break;
        }
        refresh();
    }

    private void apply() {
        // The values live in persistent properties; bumping the sequence number makes init run the root script.
        Props.set(Tweaks.PROP_ZRAM, String.valueOf(zram));
        Props.set(Tweaks.PROP_SWAPPINESS, String.valueOf(swappiness));
        Props.set(Tweaks.PROP_SWAPFILE, String.valueOf(swapfile));
        Props.set(Tweaks.PROP_SEQ, String.valueOf(System.currentTimeMillis() / 1000));
        status.setText("Applying...");
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                String r = Props.get("sys.jgm.apply_result", "");
                status.setText(r.isEmpty() ? "Sent. It can take a few seconds." : r);
            }
        }, 3500);
    }

    @Override public boolean onKeyDown(int code, KeyEvent e) {
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_UP: selected = (selected + ROWS - 1) % ROWS; refresh(); return true;
            case KeyEvent.KEYCODE_DPAD_DOWN: selected = (selected + 1) % ROWS; refresh(); return true;
            case KeyEvent.KEYCODE_DPAD_LEFT: change(-1); return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: change(1); return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (selected == ROW_APPLY) apply(); else change(1);
                return true;
            default: return super.onKeyDown(code, e);
        }
    }
}
