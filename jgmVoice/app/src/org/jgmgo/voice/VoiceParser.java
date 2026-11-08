package org.jgmgo.voice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure logic: turns what was said into a command. A port of the command parser from JVoice (jgmOS), English only. */
public final class VoiceParser {
    private VoiceParser() {}
    public enum Type { CALL, SMS, OPEN, TIME, DATE, BATTERY, FLASHLIGHT, WIFI, BLUETOOTH, VOLUME, TIMER, ALARM, CANCEL, HELP, UNKNOWN }

    public static final class Cmd {
        public final Type type; public final String name, body; public final Boolean on; public final long ms; public final int hour, minute;
        Cmd(Type t, String name, String body, Boolean on, long ms, int h, int m) { type = t; this.name = name; this.body = body; this.on = on; this.ms = ms; hour = h; minute = m; }
        @Override public String toString() { return type + (name != null ? " name=" + name : "") + (body != null ? " body=" + body : "") + (on != null ? " on=" + on : "") + (ms > 0 ? " ms=" + ms : "") + (hour >= 0 ? " " + hour + ":" + (minute < 10 ? "0" : "") + minute : ""); }
    }
    private static Cmd of(Type t) { return new Cmd(t, null, null, null, 0, -1, 0); }

    private static final String[] ONES = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"};
    private static final String[] TENS = {"", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"};

    /** "five" -> 5, "twenty five" -> 25, "7" -> 7, "a"/"an" -> 1, "half" -> -1 (only used for "half an hour"); null if not a number. */
    static Integer number(String w) {
        w = w.trim();
        if (w.matches("\\d+")) return Integer.parseInt(w);
        if (w.equals("a") || w.equals("an")) return 1;
        for (int i = 0; i < ONES.length; i++) if (ONES[i].equals(w)) return i;
        for (int i = 2; i < TENS.length; i++) {
            if (TENS[i].equals(w)) return i * 10;
            if (w.startsWith(TENS[i] + " ")) { Integer r = number(w.substring(TENS[i].length() + 1)); if (r != null && r > 0 && r < 10) return i * 10 + r; }
        }
        return null;
    }

    /** "five minutes", "half an hour", "an hour and a half", "ninety seconds", "1 hour 30 minutes" -> milliseconds, or 0. */
    static long duration(String t) {
        t = t.replaceAll("\\band\\b", " ").replaceAll("\\s+", " ").trim();
        if (t.matches("half an hour|half hour")) return 30 * 60000L;
        if (t.matches("an hour and a half|an hour a half|one and a half hours?")) return 90 * 60000L;
        Matcher m = Pattern.compile("((?:\\d+|[a-z]+(?: [a-z]+)?)) (second|seconds|sec|secs|minute|minutes|min|mins|hour|hours)\\b").matcher(t);
        long total = 0; int found = 0, end = 0;
        while (m.find()) {
            Integer n = number(m.group(1));
            if (n == null) return 0;
            String u = m.group(2);
            total += n * (u.startsWith("h") ? 3600000L : u.startsWith("m") ? 60000L : 1000L);
            found++; end = m.end();
        }
        return found > 0 && t.substring(end).trim().isEmpty() ? total : 0;
    }

    /** "7", "7 30", "seven thirty", "7 30 am", "seven o clock p m", "7:30" -> {hour, minute} or null. */
    static int[] clock(String t) {
        t = t.replace(":", " ").replaceAll("\\.", "").replaceAll("\\s+", " ").trim();
        Character ap = null;
        Matcher am = Pattern.compile(" ?(a m|p m|am|pm)$").matcher(t);
        if (am.find()) { ap = am.group(1).charAt(0); t = t.substring(0, am.start()); }
        t = t.replaceAll(" o clock$| oclock$", "");
        int h, mi = 0;
        String[] w = t.split(" ");
        Integer a = number(w[0]);
        if (a == null) return null;
        h = a;
        if (w.length > 1) {
            String rest = t.substring(w[0].length() + 1);
            if (rest.startsWith("oh ")) rest = rest.substring(3);
            Integer b = number(rest);
            if (b == null) return null;
            mi = b;
        }
        if (ap != null) { if (h == 12) h = 0; if (ap == 'p') h += 12; }
        return h < 24 && mi < 60 ? new int[] {h, mi} : null;
    }

    public static Cmd parse(String said) {
        String t = said == null ? "" : said.toLowerCase().replaceAll("wi-fi", "wifi").replaceAll("[^a-z0-9' :]", " ").replaceAll("\\s+", " ").trim();
        t = t.replaceFirst("^(?:(?:please|could you|can you|would you|will you|hey|okay|ok|jgm|assistant) )+", "").replaceFirst("(?: (?:please|now|for me|thanks|thank you))+$", "").trim();
        if (t.isEmpty()) return of(Type.UNKNOWN);
        Matcher m;
        if (t.matches("cancel|stop|never mind|forget it|nothing|abort")) return of(Type.CANCEL);
        if (t.matches("help|what can (?:you|i) (?:do|say|ask)|what do you know|commands")) return of(Type.HELP);
        if (t.matches("(?:what(?:'s| is) )?(?:the )?(?:current )?time(?: is it| now)?|what time is it|got the time|what is the time")) return of(Type.TIME);
        if (t.matches("(?:what(?:'s| is) )?(?:the )?(?:date|day)(?: today| is it)?|what day is it|what is today|today's date")) return of(Type.DATE);
        if (t.contains("battery")) return of(Type.BATTERY);
        // alarm and timer before the generic toggles
        if ((m = Pattern.compile("^(?:set )?(?:an? )?alarm (?:for|at) (.+)$|^wake me up at (.+)$").matcher(t)).find()) {
            int[] c = clock(m.group(1) != null ? m.group(1) : m.group(2));
            if (c != null) return new Cmd(Type.ALARM, null, null, null, 0, c[0], c[1]);
        }
        if ((m = Pattern.compile("^(?:set )?(?:an? )?timer (?:for )?(.+)$|^(?:remind me in|countdown (?:for|from)?|set a reminder in) (.+)$|^(?:set )?(?:an? )?(.+) timer$").matcher(t)).find()) {
            String d = m.group(1) != null ? m.group(1) : m.group(2) != null ? m.group(2) : m.group(3);
            long ms = duration(d);
            if (ms > 0) return new Cmd(Type.TIMER, null, null, null, ms, -1, 0);
        }
        Boolean on = state(t);
        if (t.matches(".*\\b(?:torch|flashlight|flash light|flash|lamp)\\b.*")) return new Cmd(Type.FLASHLIGHT, null, null, on, 0, -1, 0);
        if (t.matches(".*\\b(?:wifi|wi fi|wireless|wlan)\\b.*")) return new Cmd(Type.WIFI, null, null, on, 0, -1, 0);
        if (t.matches(".*\\bbluetooth\\b.*")) return new Cmd(Type.BLUETOOTH, null, null, on, 0, -1, 0);
        if (t.matches("volume up|louder|turn (?:it |the volume )?up|raise (?:the )?volume|increase (?:the )?volume")) return new Cmd(Type.VOLUME, null, null, true, 0, -1, 0);
        if (t.matches("volume down|quieter|turn (?:it |the volume )?down|lower (?:the )?volume|decrease (?:the )?volume")) return new Cmd(Type.VOLUME, null, null, false, 0, -1, 0);
        // messages: "text anna saying i am late", "tell anna that i am late", "send a message to anna"
        if ((m = Pattern.compile("^(?:text|message|sms|send (?:a |an )?(?:text|message|sms) to|write to) (.+?)(?: (?:saying|that says|with the message|and say|that) (.+))?$").matcher(t)).find())
            return new Cmd(Type.SMS, m.group(1).trim(), m.group(2), null, 0, -1, 0);
        if ((m = Pattern.compile("^tell (.+?) (?:that|to) (.+)$").matcher(t)).find()) return new Cmd(Type.SMS, m.group(1).trim(), m.group(2), null, 0, -1, 0);
        if ((m = Pattern.compile("^(?:call|phone|dial|ring up|ring|give a call to|make a call to|get (.+?) on the phone)(?: (.+))?$").matcher(t)).find()) {
            String n = m.group(1) != null ? m.group(1) : m.group(2);
            if (n != null && !n.isEmpty()) return new Cmd(Type.CALL, n.trim(), null, null, 0, -1, 0);
        }
        if ((m = Pattern.compile("^(?:open|launch|start|run|go to|bring up|pull up|show me) (?:the )?(.+?)(?: app)?$").matcher(t)).find()) return new Cmd(Type.OPEN, m.group(1).trim(), null, null, 0, -1, 0);
        return new Cmd(Type.UNKNOWN, t, null, null, 0, -1, 0);
    }

    /** on / off words anywhere in the sentence; null = toggle. */
    private static Boolean state(String t) {
        boolean on = t.matches(".*\\b(?:on|enable|activate|start|switch on|turn on)\\b.*"), off = t.matches(".*\\b(?:off|disable|deactivate|stop|kill|cut)\\b.*");
        return on == off ? null : Boolean.valueOf(on);
    }
}
