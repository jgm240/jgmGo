package org.jgmgo.keys;

import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;

/** Multi-tap typing with the number keys: 2 = a b c, press again to cycle, # changes Abc / abc / ABC / 123, 0 = space. */
public class T9Ime extends InputMethodService {
    private final MultiTap tap = new MultiTap();
    private final Handler handler = new Handler();
    private TextView strip;
    private final Runnable commit = new Runnable() {
        @Override public void run() { InputConnection ic = getCurrentInputConnection(); if (ic != null) ic.finishComposingText(); tap.commit(); }
    };

    @Override public boolean onEvaluateInputViewShown() { return true; }      // the phone has a keypad, but still show the mode strip

    @Override public View onCreateInputView() {
        strip = new TextView(this);
        strip.setTextSize(14); strip.setPadding(8, 4, 8, 4); strip.setTextColor(0xFFF5F7FA); strip.setBackgroundColor(0xFF0C1218);
        showMode();
        return strip;
    }

    private void showMode() {
        if (strip == null) return;
        String m = tap.mode() == MultiTap.Mode.NUM ? "123" : tap.mode() == MultiTap.Mode.UPPER ? "ABC" : tap.mode() == MultiTap.Mode.LOWER ? "abc" : "Abc";
        strip.setText("T9  " + m + "   # = mode   0 = space");
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return super.onKeyDown(keyCode, event);
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9 && event.getRepeatCount() == 0) {
            MultiTap.Edit e = tap.press(keyCode - KeyEvent.KEYCODE_0, event.getEventTime());
            if (tap.mode() == MultiTap.Mode.NUM) { ic.commitText(e.insert, 1); tap.commit(); }
            else {
                if (e.deleteBefore == 0) ic.finishComposingText();
                ic.setComposingText(e.insert, 1);
                handler.removeCallbacks(commit); handler.postDelayed(commit, MultiTap.TIMEOUT_MS);
            }
            showMode();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_POUND) { ic.finishComposingText(); tap.cycleMode(); showMode(); return true; }
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            handler.removeCallbacks(commit); tap.commit();
            ic.finishComposingText(); ic.deleteSurroundingText(1, 0);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
