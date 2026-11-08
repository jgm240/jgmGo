import org.jgmgo.launcher.Favorites;
import java.util.*;

public class LauncherTests {
    static int n = 0;
    static void eq(Object a, Object b, String w) { if (!String.valueOf(a).equals(String.valueOf(b))) throw new Error(w + ": got " + a + " expected " + b); n++; }
    public static void main(String[] x) {
        Favorites f = new Favorites();
        List<String> apps = Arrays.asList("com.android.dialer/.Main", "com.android.messaging/.Main", "com.android.contacts/.Main", "org.x.camera/.Main", "com.android.settings/.Main", "com.android.deskclock/.Main");
        f.fillDefaults(apps, new String[] {"dialer", "messaging", "contacts", "camera", "settings", "deskclock", "calculator", "browser"});
        eq(f.get(2), "com.android.dialer/.Main", "first favourite on 2"); eq(f.get(7), "com.android.deskclock/.Main", "six apps on 2..7"); eq(f.get(8), null, "missing ones are skipped");
        f.set(5, "com.android.dialer/.Main"); eq(f.get(2), null, "an app can hold only one key"); eq(f.get(5), "com.android.dialer/.Main", "it moved to 5");
        String s = f.serialize(); Favorites g = Favorites.parse(s); eq(g.serialize(), s, "round trip"); eq(g.get(5), "com.android.dialer/.Main", "parsed");
        eq(Favorites.parse("junk;1=a;2=b").get(2), "b", "junk ignored"); eq(Favorites.parse("1=a").get(1), null, "1 is not assignable"); eq(Favorites.parse(null).serialize(), "", "null is empty");
        boolean threw = false; try { f.set(1, "x"); } catch (IllegalArgumentException e) { threw = true; } eq(threw, true, "1 rejected");
        f.clear(5); eq(f.get(5), null, "cleared");
        System.out.println(n + " launcher checks passed");
    }
}
