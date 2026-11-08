package org.jgmgo.keys;

import java.util.List;

/** Pure logic: in a long list, press a number key to jump to the next item that starts with one of its letters. */
public final class TypeAhead {
    private TypeAhead() {}
    private static final String[] LETTERS = {"", "", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz"};

    public static boolean isLetterKey(int digit) { return digit >= 2 && digit <= 9; }

    /** Next index after {@code cur} (wrapping) whose label starts with a letter of this key; -1 if none. */
    public static int next(List<String> labels, int cur, int digit) {
        if (!isLetterKey(digit) || labels.isEmpty()) return -1;
        String set = LETTERS[digit];
        for (int k = 1; k <= labels.size(); k++) {
            int i = (Math.max(cur, -1) + k) % labels.size();
            String l = labels.get(i);
            if (l != null && !l.isEmpty() && set.indexOf(Character.toLowerCase(l.charAt(0))) >= 0) return i;
        }
        return -1;
    }
}
