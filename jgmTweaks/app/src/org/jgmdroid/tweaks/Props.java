package org.jgmdroid.tweaks;

/** android.os.SystemProperties is hidden from the public SDK; reflection lets this compile anywhere (the app is platform-signed, so it works). */
final class Props {
    private Props() {}
    static int getInt(String k, int def) { try { return (Integer) cls().getMethod("getInt", String.class, int.class).invoke(null, k, def); } catch (Throwable t) { return def; } }
    static boolean getBool(String k, boolean def) { try { return (Boolean) cls().getMethod("getBoolean", String.class, boolean.class).invoke(null, k, def); } catch (Throwable t) { return def; } }
    static String get(String k, String def) { try { return (String) cls().getMethod("get", String.class, String.class).invoke(null, k, def); } catch (Throwable t) { return def; } }
    static void set(String k, String v) { try { cls().getMethod("set", String.class, String.class).invoke(null, k, v); } catch (Throwable t) { /* not allowed: ignore */ } }
    private static Class<?> cls() throws ClassNotFoundException { return Class.forName("android.os.SystemProperties"); }
}
