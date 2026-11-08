package org.jgmgo.keys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure logic: predictive T9. Type the keys once each (4663) and get the likeliest words (good, home, gone, hood ...).
 * Words come most-frequent-first from the dictionary, so the first candidate is the best guess.
 */
public final class T9Predictor {
    private static final String[] GROUPS = {"", "", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz"};
    private final Map<String, List<String>> exact = new HashMap<>();
    private final List<String> words = new ArrayList<>();
    private final List<String> codes = new ArrayList<>();

    /** Key digits for a word; apostrophes are skipped (don't = 3668). Returns null if the word has other characters. */
    public static String codeOf(String word) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = Character.toLowerCase(word.charAt(i));
            if (c == '\'') continue;
            int g = -1;
            for (int k = 2; k <= 9; k++) if (GROUPS[k].indexOf(c) >= 0) { g = k; break; }
            if (g < 0) return null;
            sb.append((char) ('0' + g));
        }
        return sb.toString();
    }

    /** @param lines "word<TAB>frequency", most frequent first */
    public T9Predictor(Iterable<String> lines) {
        for (String line : lines) {
            int t = line.indexOf('\t');
            String w = t < 0 ? line.trim() : line.substring(0, t).trim();
            if (w.isEmpty()) continue;
            String code = codeOf(w);
            if (code == null || code.isEmpty()) continue;
            List<String> l = exact.get(code);
            if (l == null) { l = new ArrayList<>(2); exact.put(code, l); }
            l.add(w);
            words.add(w); codes.add(code);
        }
    }
    public int size() { return words.size(); }

    /** Whole-word matches first, then longer words that start with these keys (completions), at most {@code max}. */
    public List<String> candidates(String digits, int max) {
        List<String> out = new ArrayList<>();
        if (digits.isEmpty()) return out;
        List<String> e = exact.get(digits);
        if (e != null) for (String w : e) { if (out.size() >= max) return out; out.add(w); }
        for (int i = 0; i < words.size() && out.size() < max; i++) {
            String c = codes.get(i);
            if (c.length() > digits.length() && c.startsWith(digits)) out.add(words.get(i));
        }
        return out;
    }

    /** The word being typed: the keys so far, the candidate list and which candidate is showing. */
    public static final class Word {
        private final T9Predictor p;
        private final StringBuilder digits = new StringBuilder();
        private List<String> cands = new ArrayList<>();
        private int index = 0;
        private boolean capFirst;
        public Word(T9Predictor p) { this.p = p; }

        public void start(boolean capitaliseFirst) { digits.setLength(0); cands = new ArrayList<>(); index = 0; capFirst = capitaliseFirst; }
        public boolean active() { return digits.length() > 0; }
        public String digits() { return digits.toString(); }
        public void press(int digit) {
            if (digit < 2 || digit > 9) throw new IllegalArgumentException("digit");
            digits.append((char) ('0' + digit)); refresh();
        }
        /** Remove the last key; true if something was removed. */
        public boolean backspace() {
            if (digits.length() == 0) return false;
            digits.setLength(digits.length() - 1); refresh(); return true;
        }
        public void nextCandidate() { if (!cands.isEmpty()) index = (index + 1) % cands.size(); }
        public void previousCandidate() { if (!cands.isEmpty()) index = (index + cands.size() - 1) % cands.size(); }
        public boolean hasMatch() { return !cands.isEmpty(); }
        public List<String> candidates() { return cands; }
        public int index() { return index; }
        /** What to show in the text box while composing. With no match, the first letter of each key (a placeholder). */
        public String text() {
            String w;
            if (cands.isEmpty()) { StringBuilder sb = new StringBuilder(); for (int i = 0; i < digits.length(); i++) sb.append(GROUPS[digits.charAt(i) - '0'].charAt(0)); w = sb.toString(); }
            else w = cands.get(index);
            return shape(w);
        }
        private String shape(String w) {
            if (w.equals("i")) return "I";
            if (w.startsWith("i'")) return "I" + w.substring(1);
            return capFirst && !w.isEmpty() ? Character.toUpperCase(w.charAt(0)) + w.substring(1) : w;
        }
        private void refresh() { cands = p.candidates(digits.toString(), 12); index = 0; }
    }
}
