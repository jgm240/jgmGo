import org.jgmgo.voice.VoiceParser;
import org.jgmgo.voice.VoiceParser.Cmd;

public class VoiceTests {
    static int n = 0;
    static void eq(String said, String expect) { String got = VoiceParser.parse(said).toString(); if (!got.equals(expect)) throw new Error("\"" + said + "\": got " + got + " expected " + expect); n++; }
    public static void main(String[] x) {
        eq("call mom", "CALL name=mom"); eq("please call Anna Smith", "CALL name=anna smith"); eq("ring up dad", "CALL name=dad"); eq("get tom on the phone", "CALL name=tom"); eq("dial john", "CALL name=john");
        eq("text anna saying I am late", "SMS name=anna body=i am late"); eq("message tom", "SMS name=tom"); eq("tell mom that dinner is ready", "SMS name=mom body=dinner is ready");
        eq("send a text to dad", "SMS name=dad");
        eq("open the camera", "OPEN name=camera"); eq("launch calculator app", "OPEN name=calculator"); eq("bring up settings", "OPEN name=settings");
        eq("what time is it", "TIME"); eq("what's the time", "TIME"); eq("got the time", "TIME"); eq("what day is it", "DATE"); eq("what is the date", "DATE");
        eq("how much battery do I have", "BATTERY"); eq("battery", "BATTERY");
        eq("turn on the flashlight", "FLASHLIGHT on=true"); eq("turn the flashlight off", "FLASHLIGHT on=false"); eq("torch", "FLASHLIGHT"); eq("switch off the flash light", "FLASHLIGHT on=false");
        eq("turn on wifi", "WIFI on=true"); eq("disable wireless", "WIFI on=false"); eq("turn off Wi-Fi", "WIFI on=false"); eq("bluetooth on", "BLUETOOTH on=true"); eq("turn bluetooth off", "BLUETOOTH on=false");
        eq("volume up", "VOLUME on=true"); eq("turn it down", "VOLUME on=false"); eq("louder", "VOLUME on=true");
        eq("set a timer for five minutes", "TIMER ms=300000"); eq("timer for ten minutes", "TIMER ms=600000"); eq("set a 5 minute timer", "TIMER ms=300000");
        eq("remind me in half an hour", "TIMER ms=1800000"); eq("set a timer for ninety seconds", "TIMER ms=90000"); eq("timer for an hour and a half", "TIMER ms=5400000"); eq("remind me in twenty five minutes", "TIMER ms=1500000");
        eq("set an alarm for seven thirty a m", "ALARM 7:30"); eq("wake me up at 6 am", "ALARM 6:00"); eq("alarm for eight p m", "ALARM 20:00"); eq("set an alarm for seven oh five a m", "ALARM 7:05"); eq("set an alarm at 7:30", "ALARM 7:30"); eq("set an alarm for twelve a m", "ALARM 0:00");
        eq("cancel", "CANCEL"); eq("never mind", "CANCEL"); eq("what can I say", "HELP"); eq("help", "HELP");
        eq("blah blah", "UNKNOWN name=blah blah"); eq("", "UNKNOWN"); eq("set a timer for a banana", "UNKNOWN name=set a timer for a banana");
        System.out.println(n + " voice checks passed");
    }
}
