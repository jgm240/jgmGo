#!/usr/bin/env python3
"""Builds app/assets/t9_en.txt (word<TAB>frequency, most frequent first) from the AOSP LatinIME English word list.
Usage: make_t9_dict.py path/to/en_US_wordlist.combined.gz out.txt [min_freq]
Source data: packages/inputmethods/LatinIME/dictionaries (Apache License 2.0, The Android Open Source Project)."""
import gzip, re, sys
src, dst = sys.argv[1], sys.argv[2]
min_f = int(sys.argv[3]) if len(sys.argv) > 3 else 40
seen = {}
for line in gzip.open(src, 'rt', encoding='utf-8'):
    line = line.strip()
    if not line.startswith('word='):
        continue
    f = dict(p.split('=', 1) for p in line.split(',') if '=' in p)
    w, freq, flags = f.get('word', ''), int(f.get('f', '0')), f.get('flags', '')
    if 'not_a_word' in flags or 'possibly_offensive' in flags or 'blacklisted' in flags:
        continue
    if not re.fullmatch(r"[a-z]+(?:'[a-z]+)?", w):     # lowercase words only (names are capitalised in the source)
        continue
    if freq >= min_f and (w not in seen or freq > seen[w]):
        seen[w] = freq
words = sorted(seen.items(), key=lambda kv: (-kv[1], kv[0]))
with open(dst, 'w') as out:
    for w, fr in words:
        out.write(f"{w}\t{fr}\n")
print(len(words), "words")
