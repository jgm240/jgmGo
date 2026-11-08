package org.jgmgo.keys;

import android.content.Context;
import android.os.PowerManager;
import android.os.SystemClock;

/** The few platform calls that are hidden from the public SDK (this app is platform-signed, so they work). Reflection keeps it compiling anywhere. */
final class Hidden {
    private Hidden() {}
    static void sleepNow(Context c) { call(c.getSystemService(Context.POWER_SERVICE), "goToSleep", SystemClock.uptimeMillis()); }
    static void wakeNow(Context c) { call(c.getSystemService(Context.POWER_SERVICE), "wakeUp", SystemClock.uptimeMillis()); }
    static boolean endCall(Context c) { return Boolean.TRUE.equals(call(c.getSystemService(Context.TELECOM_SERVICE), "endCall")); }
    static void answerCall(Context c) { call(c.getSystemService(Context.TELECOM_SERVICE), "acceptRingingCall"); }
    static boolean ringing(Context c) { return Boolean.TRUE.equals(call(c.getSystemService(Context.TELECOM_SERVICE), "isRinging")); }
    static boolean inCall(Context c) { return Boolean.TRUE.equals(call(c.getSystemService(Context.TELECOM_SERVICE), "isInCall")); }
    static boolean getBoolProp(String key, boolean def) {
        try { Class<?> sp = Class.forName("android.os.SystemProperties"); return (Boolean) sp.getMethod("getBoolean", String.class, boolean.class).invoke(null, key, def); }
        catch (Throwable t) { return def; }
    }
    static int getIntProp(String key, int def) {
        try { Class<?> sp = Class.forName("android.os.SystemProperties"); return (Integer) sp.getMethod("getInt", String.class, int.class).invoke(null, key, def); }
        catch (Throwable t) { return def; }
    }
    private static Object call(Object target, String name, Object... args) {
        if (target == null) return null;
        try {
            for (java.lang.reflect.Method m : target.getClass().getMethods()) {
                if (!m.getName().equals(name) || m.getParameterTypes().length != args.length) continue;
                return m.invoke(target, args);
            }
        } catch (Throwable t) { /* not available on this build: ignore */ }
        return null;
    }
}
