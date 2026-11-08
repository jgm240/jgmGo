package org.jgmgo.voice;

/** Speech recognition through the Vosk library (offline). The model sits in the system image; see device/README. */
final class Vosk implements AutoCloseable {
    static { System.loadLibrary("jgmvosk"); }
    private static native long nativeLoad(String path);
    private static native long nativeRecognizer(long model, float rate);
    private static native int nativeAccept(long rec, byte[] data, int len);
    private static native String nativeResult(long rec);
    private static native String nativeFinal(long rec);
    private static native String nativePartial(long rec);
    private static native void nativeFree(long rec, long model);

    static final String MODEL = "/system/etc/jgmvoice/model";
    private long model, rec;

    static Vosk open() { Vosk v = new Vosk(); v.model = nativeLoad(MODEL); if (v.model == 0) return null; v.rec = nativeRecognizer(v.model, 16000f); return v.rec == 0 ? null : v; }

    /** Feed 16 kHz mono 16-bit audio. Returns the sentence when one just ended, else null. */
    String feed(byte[] pcm, int len) { return nativeAccept(rec, pcm, len) == 1 ? textOf(nativeResult(rec)) : null; }
    String partial() { return textOf(nativePartial(rec).replace("\"partial\"", "\"text\"")); }
    String finish() { return textOf(nativeFinal(rec)); }
    @Override public void close() { nativeFree(rec, model); rec = 0; model = 0; }

    /** {"text" : "hello world"} -> hello world */
    static String textOf(String json) {
        int i = json.indexOf("\"text\""); if (i < 0) return "";
        int a = json.indexOf('"', json.indexOf(':', i) + 1), b = a < 0 ? -1 : json.indexOf('"', a + 1);
        return a < 0 || b < 0 ? "" : json.substring(a + 1, b);
    }
}
