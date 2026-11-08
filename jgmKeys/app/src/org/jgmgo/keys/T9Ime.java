package org.jgmgo.keys;

import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Keypad keyboard. # cycles Abc, abc, ABC, 123 and Predictive on the fly.
 * Multi-tap: 2 = a b c, press again to cycle. Predictive: press each key once (4663 = good, home, gone ...), * = next
 * word, 0 = space, 1 = punctuation. Number fields switch to 123 and password / address fields to plain abc by themselves.
 */
public class T9Ime extends InputMethodService {
    private final MultiTap tap = new MultiTap();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView strip;
    private volatile T9Predictor predictor;
    private T9Predictor.Word word;
    private MultiTap.Mode userMode = MultiTap.Mode.CAP_NEXT, forced = null;
    private final Runnable commitTap = new Runnable() {
        @Override public void run() { InputConnection ic = getCurrentInputConnection(); if (ic != null) ic.finishComposingText(); tap.commit(); }
    };

    @Override public void onCreate() {
        super.onCreate();
        new Thread(new Runnable() {          // the word list is read once, off the main thread
            @Override public void run() {
                List<String> lines = new ArrayList<>();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(getAssets().open("t9_en.txt"), "UTF-8"))) {
                    String l; while ((l = r.readLine()) != null) lines.add(l);
                } catch (Exception e) { return; }
                final T9Predictor p = new T9Predictor(lines);
                handler.post(new Runnable() { @Override public void run() { predictor = p; word = new T9Predictor.Word(p); showMode(); } });
            }
        }).start();
    }

    @Override public boolean onEvaluateInputViewShown() { return true; }      // the phone has a keypad, but still show the strip

    @Override public View onCreateInputView() {
        strip = new TextView(this);
        strip.setTextSize(13); strip.setPadding(8, 4, 8, 4); strip.setTextColor(0xFFF5F7FA); strip.setBackgroundColor(0xFF0C1218);
        strip.setMaxLines(2);
        showMode();
        return strip;
    }

    @Override public void onStartInput(EditorInfo info, boolean restarting) {
        super.onStartInput(info, restarting);
        commitWord();
        int cls = info == null ? 0 : info.inputType & InputType.TYPE_MASK_CLASS, var = info == null ? 0 : info.inputType & InputType.TYPE_MASK_VARIATION;
        MultiTap.Mode f = null;
        if (cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE || cls == InputType.TYPE_CLASS_DATETIME) f = MultiTap.Mode.NUM;
        else if (cls == InputType.TYPE_CLASS_TEXT && (var == InputType.TYPE_TEXT_VARIATION_PASSWORD || var == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || var == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD || var == InputType.TYPE_TEXT_VARIATION_URI || var == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)) f = MultiTap.Mode.LOWER;
        if (forced == null) userMode = tap.mode();        // remember what the user had chosen
        forced = f;
        tap.setMode(f != null ? f : userMode);
        showMode();
    }
    @Override public void onFinishInput() { commitWord(); super.onFinishInput(); }

    private MultiTap.Mode mode() { return tap.mode() == MultiTap.Mode.PREDICT && predictor == null ? MultiTap.Mode.CAP_NEXT : tap.mode(); }

    private void showMode() {
        String m = MultiTap.label(tap.mode());
        ModeState.typingMode = m;
        if (strip == null) return;
        StringBuilder sb = new StringBuilder("T9  ").append(m).append(forced != null ? "  (this field)" : "").append("    # = mode");
        if (tap.mode() == MultiTap.Mode.PREDICT) {
            if (predictor == null) sb.append("\n(loading words...)");
            else if (word != null && word.active()) {
                sb.append("\n"); List<String> c = word.candidates();
                if (c.isEmpty()) sb.append("no word: ").append(word.text());
                for (int i = 0; i < c.size() && i < 5; i++) sb.append(i == word.index() ? "[" + c.get(i) + "] " : c.get(i) + " ");
                if (c.size() > 1) sb.append("  * = next");
            } else sb.append("\n0 = space   1 = . , ? !");
        }
        strip.setText(sb.toString());
    }

    private void commitWord() {
        if (word != null && word.active()) {
            InputConnection ic = getCurrentInputConnection(); if (ic != null) ic.finishComposingText();
            word.start(false);
        }
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return super.onKeyDown(keyCode, event);
        boolean predictive = tap.mode() == MultiTap.Mode.PREDICT && predictor != null && word != null;
        boolean digit = keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9;
        int d = keyCode - KeyEvent.KEYCODE_0;

        if (keyCode == KeyEvent.KEYCODE_POUND) {                          // # : change the typing mode on the fly
            if (event.getRepeatCount() != 0) return true;
            commitWord(); ic.finishComposingText();
            tap.cycleMode(); if (forced == null) userMode = tap.mode();
            showMode();
            return true;
        }
        if (predictive) {
            if (digit && event.getRepeatCount() == 0) {
                if (d >= 2) {
                    if (!word.active()) word.start(ic.getCursorCapsMode(TextUtils.CAP_MODE_SENTENCES) != 0);
                    word.press(d);
                    ic.setComposingText(word.text(), 1);
                    showMode(); return true;
                }
                commitWord();
                if (d == 0) { ic.commitText(" ", 1); showMode(); return true; }
                // d == 1: punctuation, cycled like multi-tap
                MultiTap.Edit e = tap.press(1, event.getEventTime());
                if (e.deleteBefore == 0) ic.finishComposingText();
                ic.setComposingText(e.insert, 1);
                handler.removeCallbacks(commitTap); handler.postDelayed(commitTap, MultiTap.TIMEOUT_MS);
                showMode(); return true;
            }
            if (keyCode == KeyEvent.KEYCODE_STAR && word.active()) {      // * : next candidate word
                word.nextCandidate(); ic.setComposingText(word.text(), 1); showMode(); return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                if (word.active()) {
                    word.backspace();
                    if (word.active()) ic.setComposingText(word.text(), 1); else { ic.setComposingText("", 1); ic.finishComposingText(); }
                    showMode(); return true;
                }
                ic.deleteSurroundingText(1, 0); return true;
            }
            commitWord();            // any other key ends the word, then does its normal job
            showMode();
            return super.onKeyDown(keyCode, event);
        }
        // multi-tap, number and fallback modes
        if (digit && event.getRepeatCount() == 0) {
            MultiTap.Edit e = tap.press(d, event.getEventTime());
            if (tap.mode() == MultiTap.Mode.NUM) { ic.commitText(e.insert, 1); tap.commit(); }
            else {
                if (e.deleteBefore == 0) ic.finishComposingText();
                ic.setComposingText(e.insert, 1);
                handler.removeCallbacks(commitTap); handler.postDelayed(commitTap, MultiTap.TIMEOUT_MS);
            }
            showMode();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            handler.removeCallbacks(commitTap); tap.commit();
            ic.finishComposingText(); ic.deleteSurroundingText(1, 0);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
