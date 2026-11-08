# jgmGo boot animation: a droid in the jgmOS colours opens its eyes and raises a Nokia 8110 4G.
import math, os, shutil, zipfile
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageChops
W, H, SS = 240, 320, 4
import os
FONT = os.environ.get('JGM_FONT', os.path.join(os.path.dirname(os.path.abspath(__file__)), 'fonts', 'DejaVuSans-Bold.ttf'))   # DejaVu Sans Bold, freely redistributable
BG_TOP, BG_BOT = (8, 12, 19), (12, 24, 30)
ACC = (45, 224, 165); ACC_D = (28, 160, 118); WHITE = (245, 247, 250); NAVY = (10, 22, 30)
def font(sz, w=b'Heavy'):
    f = ImageFont.truetype(FONT, int(sz * SS))
    try: f.set_variation_by_name(w)
    except Exception: pass
    return f
def ease(t): t = max(0, min(1, t)); return 1 - (1 - t) ** 3
def back(t): t = max(0, min(1, t)); c = 1.4; return 1 + (c + 1) * (t - 1) ** 3 + c * (t - 1) ** 2
def seg(t, a, b): return max(0.0, min(1.0, (t - a) / float(b - a)))
_bg = None
def background():
    global _bg
    if _bg is None:
        im = Image.new('RGB', (W * SS, H * SS)); d = ImageDraw.Draw(im)
        for y in range(H * SS):
            t = y / (H * SS); d.line([(0, y), (W * SS, y)], fill=tuple(int(BG_TOP[i] + (BG_BOT[i] - BG_TOP[i]) * t) for i in range(3)))
        g = ImageDraw.Draw(im, 'RGBA')
        for x in range(0, W * SS, 20 * SS): g.line([(x, 0), (x, H * SS)], fill=(255, 255, 255, 6))
        for y in range(0, H * SS, 20 * SS): g.line([(0, y), (W * SS, y)], fill=(255, 255, 255, 6))
        _bg = im.convert('RGBA')
    return _bg.copy()
def P(v): return v * SS
def cap(d, x0, y0, x1, y1, w, fill):          # round-capped line
    d.line((P(x0), P(y0), P(x1), P(y1)), fill=fill, width=int(P(w)))
    for x, y in ((x0, y0), (x1, y1)): d.ellipse((P(x - w / 2), P(y - w / 2), P(x + w / 2), P(y + w / 2)), fill=fill)
def phone_layer(angle, screen):
    """Nokia 8110 4G, drawn straight and square: rounded body, lit screen, then a 3x4 T9 keypad on an exact grid."""
    L = Image.new('RGBA', (60 * SS, 90 * SS), (0, 0, 0, 0)); d = ImageDraw.Draw(L)
    bx0, bx1, by0, by1 = 17, 43, 6, 82
    d.rounded_rectangle((P(bx0), P(by0), P(bx1), P(by1)), radius=P(9), fill=(14, 28, 36, 255), outline=ACC + (255,), width=int(P(1.6)))
    # screen
    col = tuple(int(NAVY[i] + (ACC[i] - NAVY[i]) * (0.15 + 0.85 * screen)) for i in range(3))
    d.rounded_rectangle((P(21), P(12), P(39), P(30)), radius=P(2), fill=col + (255,))
    if screen > 0.5: d.arc((P(26), P(17), P(34), P(25)), -50, 230, fill=NAVY + (255,), width=int(P(1.4)))
    # navigation row: two soft keys and a centre key
    for x in (24, 36): d.rounded_rectangle((P(x - 2.5), P(35), P(x + 2.5), P(38)), radius=P(1.4), fill=(70, 110, 110, 255))
    d.ellipse((P(28), P(34), P(32), P(39)), fill=ACC + (255,))
    # T9 keypad: 3 columns x 4 rows, evenly spaced
    for r in range(4):
        for c in range(3):
            x = 24 + c * 6; y = 45 + r * 8
            d.rounded_rectangle((P(x - 2.4), P(y - 2.6), P(x + 2.4), P(y + 2.6)), radius=P(1.6), fill=(60, 96, 104, 255))
    return L.rotate(angle, resample=Image.BICUBIC, center=(P(30), P(78)), expand=False)
def scene(open_=1.0, arm=1.0, screen=1.0, text=1.0, bob=0.0, wig=0.0, rise=0.0, alpha=1.0, blink=0.0):
    im = background()
    oy = bob + rise
    glow = Image.new('RGBA', im.size, (0, 0, 0, 0)); gd = ImageDraw.Draw(glow)
    gd.ellipse((P(40), P(40 + oy), P(200), P(230 + oy)), fill=ACC + (60,)); glow = glow.filter(ImageFilter.GaussianBlur(P(28)))
    im = Image.alpha_composite(im, glow)
    L = Image.new('RGBA', im.size, (0, 0, 0, 0)); d = ImageDraw.Draw(L)
    cx = 120; hy = 90 + oy       # head baseline
    g = ACC + (255,); gd_ = ACC_D + (255,)
    # legs and arms behind the body
    for x in (101, 139): cap(d, x, hy + 82, x, hy + 112, 15, g)
    # right arm: from the shoulder to the hand, hanging -> holding the phone up
    sh = (160, hy + 18); hang = (166, hy + 68); up = (188, hy + 46)
    k = back(arm); hand = (hang[0] + (up[0] - hang[0]) * k, hang[1] + (up[1] - hang[1]) * k)
    # body
    d.rounded_rectangle((P(cx - 40), P(hy + 6), P(cx + 40), P(hy + 88)), radius=P(13), fill=g)
    halo = BG_BOT + (255,)
    cap(d, 78, hy + 18, 74, hy + 66, 19.5, halo); cap(d, 78, hy + 18, 74, hy + 66, 14, g)      # left arm hangs
    cap(d, sh[0], sh[1], hand[0], hand[1], 19.5, halo); cap(d, sh[0], sh[1], hand[0], hand[1], 14, g)
    # head dome with a thin gap under it
    d.pieslice((P(cx - 40), P(hy - 38), P(cx + 40), P(hy + 38)), 180, 360, fill=g)
    d.rectangle((P(cx - 40), P(hy), P(cx + 40), P(hy + 0.5)), fill=g)
    d.line((P(cx - 41), P(hy + 3), P(cx + 41), P(hy + 3)), fill=BG_BOT + (255,), width=int(P(2.4)))
    # antennae
    for s in (-1, 1):
        cap(d, cx + s * 22, hy - 30, cx + s * (31 + wig * s), hy - 47, 4.2, g)
    # eyes: closed (sleepy arcs) -> open, with a blink
    e = open_ * (1 - blink)
    for s in (-1, 1):
        ex, ey = cx + s * 17, hy - 12
        if e < 0.12:
            d.arc((P(ex - 7), P(ey - 4), P(ex + 7), P(ey + 4)), 20, 160, fill=NAVY + (255,), width=int(P(2.4)))
        else:
            ry = 5.0 * e      # plain black dot: squashes shut for the blink, no white or highlight
            d.ellipse((P(ex - 5.0), P(ey - ry), P(ex + 5.0), P(ey + ry)), fill=(0, 0, 0, 255))
    im = Image.alpha_composite(im, L)
    # phone, held in the raised hand: grows in as the arm lifts
    if arm > 0.05:
        ang = 38 * (1 - back(arm))
        ph = phone_layer(ang, screen)
        # place so that the pivot (30,60 in layer) sits on the hand
        top = Image.new('RGBA', im.size, (0, 0, 0, 0)); top.paste(ph, (int(P(hand[0] - 30)), int(P(hand[1] - 78 + 2))), ph)
        hd = ImageDraw.Draw(top); hd.ellipse((P(hand[0] - 8), P(hand[1] - 8), P(hand[0] + 8), P(hand[1] + 8)), fill=g)   # hand over the phone
        im = Image.alpha_composite(im, top)
    # wordmark: "jgmGo" is drawn on - an outline is traced left to right by a glowing pen, then the letters fill in
    if text > 0.005:
        f = font(46); l, r = 'jgm', 'Go'
        md = ImageDraw.Draw(Image.new('L', (4, 4)))
        wl, wr = md.textlength(l, font=f), md.textlength(r, font=f); x0 = (W * SS - wl - wr) / 2; y0 = P(240)
        def layer(fill_a, stroke_a):
            T = Image.new('RGBA', im.size, (0, 0, 0, 0)); td = ImageDraw.Draw(T); sw = int(P(1.1))
            for txt, xx, col in ((l, x0, WHITE), (r, x0 + wl, ACC)):
                td.text((xx, y0), txt, font=f, fill=col + (int(255 * fill_a),), stroke_width=sw if stroke_a > 0 else 0, stroke_fill=col + (int(255 * stroke_a),))
            return T
        trace = ease(seg(text, 0.0, 0.62)); fillp = ease(seg(text, 0.5, 0.95))
        T = layer(fillp, 1.0)
        # reveal mask: everything left of the pen position, with a short soft edge
        left, right = x0 - P(4), x0 + wl + wr + P(4); penx = left + (right - left) * trace
        mask = Image.new('L', im.size, 0); md2 = ImageDraw.Draw(mask)
        md2.rectangle((0, 0, penx, im.size[1]), fill=255)
        mask = mask.filter(ImageFilter.GaussianBlur(P(2.2)))
        a = ImageChops.multiply(T.getchannel('A'), mask); T.putalpha(a)
        im = Image.alpha_composite(im, T)
        if 0.0 < trace < 1.0:                      # the pen: a soft green dot riding the baseline
            pen = Image.new('RGBA', im.size, (0, 0, 0, 0)); pd = ImageDraw.Draw(pen); py = y0 + P(38) + P(2.5 * math.sin(trace * 18))
            pd.ellipse((penx - P(5), py - P(5), penx + P(5), py + P(5)), fill=ACC + (230,)); pen = pen.filter(ImageFilter.GaussianBlur(P(2.5)))
            im = Image.alpha_composite(im, pen)
        sub_a = ease(seg(text, 0.75, 1.0))
        if sub_a > 0.01:
            T2 = Image.new('RGBA', im.size, (0, 0, 0, 0)); sd = ImageDraw.Draw(T2); sub = font(9.5, b'Semibold')
            s_ = 'for the Nokia 8110 4G'; sw_ = sd.textlength(s_, font=sub)
            sd.text(((W * SS - sw_) / 2, P(296)), s_, font=sub, fill=(150, 175, 170, int(255 * sub_a)))
            im = Image.alpha_composite(im, T2)
    out = im.convert('RGB').resize((W, H), Image.LANCZOS)
    if alpha < 1: out = Image.blend(background().convert('RGB').resize((W, H), Image.LANCZOS), out, alpha)
    return out
def intro(n=66):
    for f in range(n):
        yield scene(open_=ease(seg(f, 8, 22)), arm=seg(f, 18, 36), screen=ease(seg(f, 32, 42)), text=seg(f, 36, 64),
                    rise=(1 - ease(seg(f, 0, 9))) * 16, alpha=ease(seg(f, 0, 7)))
def loop(n=30):
    for f in range(n):
        t = f / n; blink = 0
        if 11 <= f <= 15: blink = [0.5, 1, 1, 0.6, 0.2][f - 11]
        yield scene(bob=1.4 * math.sin(2 * math.pi * t), wig=1.8 * math.sin(2 * math.pi * t + 1), screen=0.85 + 0.15 * math.sin(2 * math.pi * t), blink=blink)
if __name__ == '__main__':
    OUT = 'droid_out'; shutil.rmtree(OUT, ignore_errors=True)
    for p in ('intro', 'loop'): os.makedirs(f'{OUT}/{p}')
    I = list(intro()); Lp = list(loop())
    for i, fr in enumerate(I): fr.save(f'{OUT}/intro/frame_{i:03d}.png', optimize=True)
    for i, fr in enumerate(Lp): fr.save(f'{OUT}/loop/frame_{i:03d}.png', optimize=True)
    open(f'{OUT}/desc.txt', 'w').write('240 320 30\np 1 0 intro\np 0 0 loop\n')
    with zipfile.ZipFile('bootanimation-jgmdroid.zip', 'w', zipfile.ZIP_STORED) as z:
        z.write(f'{OUT}/desc.txt', 'desc.txt')
        for part, fs in (('intro', I), ('loop', Lp)):
            for i in range(len(fs)): z.write(f'{OUT}/{part}/frame_{i:03d}.png', f'{part}/frame_{i:03d}.png')
    I[0].save('bootanimation-jgmdroid.gif', save_all=True, append_images=I[1:] + Lp * 2, duration=33, loop=0)
    Lp[0].save('bootlogo-final.png')
    picks = [I[10], I[26], I[40], I[48], I[56], I[65]]
    sheet = Image.new('RGB', (W * 6 + 5 * 6, H), (30, 30, 30))
    for k, fr in enumerate(picks): sheet.paste(fr, (k * (W + 6), 0))
    sheet.save('droid_frames.png'); print(len(I), len(Lp), os.path.getsize('bootanimation-jgmdroid.zip') // 1024, 'KB')
