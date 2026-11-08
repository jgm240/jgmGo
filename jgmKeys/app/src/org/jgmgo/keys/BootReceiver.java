package org.jgmgo.keys;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

/** First boot: switch on the keypad navigator and the T9 keyboard, because there is no touchscreen to do it with. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        String nav = new ComponentName(c, KeyNavService.class).flattenToString();
        String ime = new ComponentName(c, T9Ime.class).flattenToString();
        String enabled = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null || !enabled.contains(nav)) {
            Settings.Secure.putString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    (enabled == null || enabled.isEmpty()) ? nav : enabled + ":" + nav);
        }
        Settings.Secure.putInt(c.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 1);
        String imes = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ENABLED_INPUT_METHODS);
        if (imes == null || !imes.contains(ime)) {
            Settings.Secure.putString(c.getContentResolver(), Settings.Secure.ENABLED_INPUT_METHODS, (imes == null || imes.isEmpty()) ? ime : imes + ":" + ime);
        }
        String def = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        if (def == null || def.isEmpty() || def.contains("latin")) {
            Settings.Secure.putString(c.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD, ime);
        }
    }
}
