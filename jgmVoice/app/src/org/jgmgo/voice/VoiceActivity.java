package org.jgmgo.voice;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.hardware.camera2.CameraManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Voice assistant: hold the Call key (Android's "voice command" key), say what you want, it does it. Everything runs on the phone.
 * The ring grows and shrinks while it listens, like the jgmOS boot animation.
 */
public class VoiceActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private TextView status, heard;
    private Orb orb;
    private TextToSpeech tts; private boolean ttsReady;
    private volatile boolean listening, cancelled;
    private String pendingSmsTo;               // asked "what should the message say?"
    private boolean torch = false;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setBackgroundColor(0xFF0C1218);
        orb = new Orb(this); root.addView(orb, new LinearLayout.LayoutParams(160, 160));
        heard = new TextView(this); heard.setTextColor(0xFFF5F7FA); heard.setTextSize(16); heard.setGravity(Gravity.CENTER); root.addView(heard);
        status = new TextView(this); status.setTextColor(0xFF9AAFA8); status.setTextSize(12); status.setGravity(Gravity.CENTER); root.addView(status);
        setContentView(root);
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() { @Override public void onInit(int s) { ttsReady = s == TextToSpeech.SUCCESS; if (ttsReady) tts.setLanguage(Locale.getDefault()); } });
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[] {android.Manifest.permission.RECORD_AUDIO}, 1);
        else listen();
    }
    @Override public void onRequestPermissionsResult(int rc, String[] p, int[] r) { if (r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) listen(); else say("I need the microphone permission."); }
    @Override protected void onDestroy() { cancelled = true; if (tts != null) tts.shutdown(); super.onDestroy(); }
    @Override public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) { if (!listening) listen(); return true; }     // OK = listen again
        return super.onKeyDown(code, e);
    }

    // ---- listening ---------------------------------------------------------------------------------------------------
    private void listen() {
        if (listening) return;
        listening = true; cancelled = false; orb.listening(true); status.setText("Listening..."); heard.setText("");
        new Thread(new Runnable() { @Override public void run() { record(); } }).start();
    }

    private void record() {
        Vosk v = Vosk.open();
        if (v == null) { post("The speech engine is not installed.", "", false); return; }
        int min = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord ar = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, 8000));
        byte[] buf = new byte[3200]; String text = null, last = "";
        try {
            ar.startRecording();
            long end = System.currentTimeMillis() + 9000;
            while (!cancelled && System.currentTimeMillis() < end) {
                int n = ar.read(buf, 0, buf.length);
                if (n <= 0) continue;
                String r = v.feed(buf, n);
                if (r != null && !r.isEmpty()) { text = r; break; }
                final String p = v.partial(); if (!p.isEmpty() && !p.equals(last)) { last = p; final String shown = p; ui.post(new Runnable() { @Override public void run() { heard.setText(shown); } }); }
            }
            if (text == null) text = v.finish();
        } finally { try { ar.stop(); } catch (Exception ignored) { /* not started */ } ar.release(); v.close(); }
        final String said = text == null ? "" : text;
        ui.post(new Runnable() { @Override public void run() { listening = false; orb.listening(false); heard.setText(said); handle(said); } });
    }

    private void post(final String s, final String h, final boolean again) { ui.post(new Runnable() { @Override public void run() { listening = false; orb.listening(false); say(s); } }); }

    private void say(String s) { status.setText(s); if (ttsReady) tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "v"); }

    // ---- doing things ------------------------------------------------------------------------------------------------
    private void handle(String said) {
        if (pendingSmsTo != null) {            // second turn: the body of a message
            String to = pendingSmsTo; pendingSmsTo = null;
            if (said.isEmpty() || VoiceParser.parse(said).type == VoiceParser.Type.CANCEL) { say("Okay, cancelled."); return; }
            sendSms(to, said); return;
        }
        VoiceParser.Cmd c = VoiceParser.parse(said);
        switch (c.type) {
            case CALL: call(c.name); break;
            case SMS: if (c.body == null) { pendingSmsTo = c.name; say("What should the message say?"); ui.postDelayed(new Runnable() { @Override public void run() { listen(); } }, 1800); } else sendSms(c.name, c.body); break;
            case OPEN: open(c.name); break;
            case TIME: say("It is " + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()) + "."); break;
            case DATE: say("Today is " + DateFormat.getDateInstance(DateFormat.FULL).format(new Date()) + "."); break;
            case BATTERY: { Intent i = registerReceiver(null, new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)); int l = i == null ? -1 : i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1); say(l < 0 ? "I can't read the battery." : "Battery is at " + l + " percent."); break; }
            case FLASHLIGHT: flashlight(c.on); break;
            case WIFI: { WifiManager w = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE); boolean on = c.on != null ? c.on : !w.isWifiEnabled(); w.setWifiEnabled(on); say("Wi-Fi " + (on ? "on" : "off") + "."); break; }
            case BLUETOOTH: { BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter(); if (a == null) { say("No Bluetooth."); break; } boolean on = c.on != null ? c.on : !a.isEnabled(); if (on) a.enable(); else a.disable(); say("Bluetooth " + (on ? "on" : "off") + "."); break; }
            case VOLUME: ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_MUSIC, c.on ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI); say(c.on ? "Louder." : "Quieter."); break;
            case TIMER: startActivity(new Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, (int) (c.ms / 1000)).putExtra(AlarmClock.EXTRA_SKIP_UI, true)); say("Timer set."); break;
            case ALARM: startActivity(new Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR, c.hour).putExtra(AlarmClock.EXTRA_MINUTES, c.minute).putExtra(AlarmClock.EXTRA_SKIP_UI, true)); say("Alarm set for " + c.hour + ":" + (c.minute < 10 ? "0" : "") + c.minute + "."); break;
            case HELP: say("Try: call mom, text anna saying hello, open camera, what time is it, turn on the flashlight, set a timer for five minutes."); break;
            case CANCEL: finish(); break;
            default: say(said.isEmpty() ? "I didn't catch that." : "I don't know how to \"" + said + "\" yet.");
        }
    }

    private String numberOf(String name) {
        try (Cursor cur = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[] {ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?", new String[] {"%" + name + "%"}, null)) {
            return cur != null && cur.moveToFirst() ? cur.getString(0) : null;
        } catch (Exception e) { return null; }
    }
    private void call(String name) {
        String n = numberOf(name); if (n == null) { say("I can't find " + name + " in your contacts."); return; }
        say("Calling " + name + "."); startActivity(new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", n, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
    private void sendSms(String name, String body) {
        String n = numberOf(name); if (n == null) { say("I can't find " + name + " in your contacts."); return; }
        try { SmsManager.getDefault().sendTextMessage(n, null, body, null, null); say("Message sent to " + name + "."); } catch (Exception e) { say("I couldn't send that message."); }
    }
    private void open(String name) {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(main, 0)) {
            String l = String.valueOf(r.loadLabel(pm)).toLowerCase(Locale.ROOT);
            if (l.equals(name) || l.contains(name) || name.contains(l)) {
                say("Opening " + r.loadLabel(pm) + ".");
                startActivity(pm.getLaunchIntentForPackage(r.activityInfo.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return;
            }
        }
        say("I can't find an app called " + name + ".");
    }
    private void flashlight(Boolean on) {
        try {
            CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            torch = on != null ? on : !torch; cm.setTorchMode(cm.getCameraIdList()[0], torch); say("Flashlight " + (torch ? "on" : "off") + ".");
        } catch (Exception e) { say("I can't reach the flashlight."); }
    }

    /** The jgmOS ring: it turns, with a gap, and breathes while listening. */
    private static final class Orb extends View {
        private final android.graphics.Paint ring = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG), core = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private boolean on; private float phase;
        Orb(Context c) { super(c); ring.setStyle(android.graphics.Paint.Style.STROKE); ring.setStrokeCap(android.graphics.Paint.Cap.ROUND); ring.setStrokeWidth(8); ring.setColor(0xFF2DE0A5); core.setColor(0xFF2DE0A5); }
        void listening(boolean v) { on = v; invalidate(); }
        @Override protected void onDraw(android.graphics.Canvas c) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f, base = Math.min(cx, cy) - 14;
            phase += 0.07f; float breathe = on ? (float) Math.sin(phase * 1.6) * 6 : 0, r = base + breathe;
            c.drawArc(cx - r, cy - r, cx + r, cy + r, (phase * 40) % 360, 290, false, ring);
            c.drawCircle(cx, cy, 14 + (on ? (float) Math.sin(phase * 1.6 + 1) * 4 : 0), core);
            postInvalidateDelayed(40);
        }
    }
}
