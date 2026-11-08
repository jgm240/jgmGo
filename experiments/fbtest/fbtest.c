// jgmOS Android-port experiment 1: can a bare kernel+init drive the SPI screen and the keypad?
#define _GNU_SOURCE
#include <stdio.h>
#include <stdarg.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <dirent.h>
#include <poll.h>
#include <time.h>
#include <sys/mman.h>
#include <sys/mount.h>
#include <sys/stat.h>
#include <sys/sysmacros.h>
#include <sys/ioctl.h>
#include <sys/reboot.h>
#include <sys/syscall.h>
#include <linux/fb.h>
#include <linux/input.h>
static FILE *lg;
static void L(const char *f, ...) { va_list a; va_start(a, f); if (lg) { vfprintf(lg, f, a); fputc('\n', lg); fflush(lg); } va_end(a); }
static unsigned short *fbm; static int fbfd, W = 240, H = 320, stride16 = 256, vH = 640;
static struct fb_var_screeninfo vi;
static unsigned short rgb(int r, int g, int b) { return ((r & 0xf8) << 8) | ((g & 0xfc) << 3) | (b >> 3); }
static void rect(int page, int x0, int y0, int x1, int y1, unsigned short c) {
  for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) fbm[(page * H + y) * stride16 + x] = c;
}
static void present(int page) {
  vi.yoffset = page * H; vi.activate = FB_ACTIVATE_NOW | FB_ACTIVATE_VBL;
  int a = ioctl(fbfd, FBIOPAN_DISPLAY, &vi); int e1 = errno;
  int b = ioctl(fbfd, FBIOPUT_VSCREENINFO, &vi); int e2 = errno;
  L("present page %d: PAN=%d(%s) PUT=%d(%s)", page, a, a ? strerror(e1) : "ok", b, b ? strerror(e2) : "ok");
}
static void pattern(int page) {
  rect(page, 0, 0, W, H, rgb(30, 30, 30));
  int bw = W / 4; unsigned short c[4] = { rgb(255, 0, 0), rgb(0, 255, 0), rgb(0, 0, 255), rgb(255, 255, 255) };
  for (int i = 0; i < 4; i++) rect(page, i * bw, 40, (i + 1) * bw, H - 40, c[i]);
  rect(page, 0, 0, W, 4, rgb(255, 255, 255)); rect(page, 0, H - 4, W, H, rgb(255, 255, 255));
  rect(page, 0, 0, 4, H, rgb(255, 255, 255)); rect(page, W - 4, 0, W, H, rgb(255, 255, 255));
  rect(page, 8, 8, 40, 32, rgb(255, 0, 255));          // top-left marker: magenta
  rect(page, W - 40, H - 32, W - 8, H - 8, rgb(0, 255, 255));   // bottom-right marker: cyan
}
static void writes(const char *p, const char *v) { int f = open(p, O_WRONLY); if (f < 0) { L("open %s: %s", p, strerror(errno)); return; } int n = write(f, v, strlen(v)); L("write %s='%s' -> %d", p, v, n); close(f); }
int main(void) {
  mkdir("/proc", 0755); mkdir("/sys", 0755); mkdir("/dev", 0755); mkdir("/cache", 0755);
  mount("proc", "/proc", "proc", 0, 0); mount("sysfs", "/sys", "sysfs", 0, 0); mount("devtmpfs", "/dev", "devtmpfs", 0, 0);
  mknod("/dev/block-cache", S_IFBLK | 0600, makedev(179, 30));
  int m = mount("/dev/block-cache", "/cache", "ext4", 0, 0);
  lg = fopen("/cache/fbtest.log", "w");
  L("jgm fbtest start; cache mount=%d (%s)", m, m ? strerror(errno) : "ok");
  { FILE *f = fopen("/proc/version", "r"); char b[300] = ""; if (f) { fgets(b, 300, f); fclose(f); } L("kernel: %s", b); }
  { FILE *f = fopen("/proc/meminfo", "r"); char b[100]; if (f) { fgets(b, 100, f); L("%s", b); fclose(f); } }
  // backlight
  DIR *d = opendir("/sys/class/leds"); struct dirent *e;
  while (d && (e = readdir(d))) if (e->d_name[0] != '.') { char p[300]; L("led: %s", e->d_name); if (strstr(e->d_name, "lcd") || strstr(e->d_name, "back") || strstr(e->d_name, "wled")) { snprintf(p, 300, "/sys/class/leds/%s/brightness", e->d_name); writes(p, "255"); } }
  if (d) closedir(d);
  d = opendir("/sys/class/backlight"); while (d && (e = readdir(d))) if (e->d_name[0] != '.') { char p[300]; L("backlight: %s", e->d_name); snprintf(p, 300, "/sys/class/backlight/%s/brightness", e->d_name); writes(p, "255"); }
  if (d) closedir(d);
  // framebuffer
  fbfd = open("/dev/graphics/fb0", O_RDWR); if (fbfd < 0) { mkdir("/dev/graphics", 0755); mknod("/dev/graphics/fb0", S_IFCHR | 0600, makedev(29, 0)); fbfd = open("/dev/graphics/fb0", O_RDWR); }
  if (fbfd < 0) { fbfd = open("/dev/fb0", O_RDWR); }
  L("fb open fd=%d (%s)", fbfd, fbfd < 0 ? strerror(errno) : "ok");
  struct fb_fix_screeninfo fi;
  if (fbfd >= 0 && ioctl(fbfd, FBIOGET_VSCREENINFO, &vi) == 0 && ioctl(fbfd, FBIOGET_FSCREENINFO, &fi) == 0) {
    L("fix: id=%.16s smem_len=%u line_length=%u type=%u", fi.id, fi.smem_len, fi.line_length, fi.type);
    L("var: %ux%u virt %ux%u bpp=%u off=%u,%u red=%u/%u green=%u/%u blue=%u/%u rotate=%u", vi.xres, vi.yres, vi.xres_virtual, vi.yres_virtual, vi.bits_per_pixel, vi.xoffset, vi.yoffset, vi.red.offset, vi.red.length, vi.green.offset, vi.green.length, vi.blue.offset, vi.blue.length, vi.rotate);
    W = vi.xres; H = vi.yres; vH = vi.yres_virtual; stride16 = fi.line_length / 2;
    int unb = ioctl(fbfd, FBIOBLANK, FB_BLANK_UNBLANK); L("unblank=%d (%s)", unb, unb ? strerror(errno) : "ok");
    fbm = mmap(0, fi.smem_len, PROT_READ | PROT_WRITE, MAP_SHARED, fbfd, 0);
    if (fbm == MAP_FAILED) { L("mmap failed: %s", strerror(errno)); fbm = 0; }
    else { int pages = vH / H; for (int p = 0; p < pages && p < 2; p++) pattern(p); present(0); }
  }
  // keypad: show a colour per key press and log every event for 20 s
  int fds[16], nf = 0; char nm[300];
  mkdir("/dev/input", 0755);
  for (int i = 0; i < 8; i++) { snprintf(nm, 300, "/dev/input/event%d", i); if (access(nm, F_OK) != 0) { int r = mknod(nm, S_IFCHR | 0600, makedev(13, 64 + i)); L("mknod %s -> %d (%s)", nm, r, r ? strerror(errno) : "ok"); } }
  { FILE *f = fopen("/proc/bus/input/devices", "r"); char b[300]; L("--- /proc/bus/input/devices"); while (f && fgets(b, 300, f)) { b[strcspn(b, "\n")] = 0; L("%s", b); } if (f) fclose(f); }
  d = opendir("/dev/input");
  while (d && (e = readdir(d)) && nf < 16) if (!strncmp(e->d_name, "event", 5)) { snprintf(nm, 300, "/dev/input/%s", e->d_name); fds[nf] = open(nm, O_RDONLY | O_NONBLOCK); if (fds[nf] >= 0) { char n[80] = ""; ioctl(fds[nf], EVIOCGNAME(80), n); L("input %s: %s", e->d_name, n); nf++; } }
  if (d) closedir(d);
  L("input devices opened: %d", nf);
  { static char kb[300000]; int n = syscall(SYS_syslog, 3, kb, sizeof kb - 1); if (n > 0) { FILE *f = fopen("/cache/dmesg.log", "w"); if (f) { fwrite(kb, 1, n, f); fclose(f); } L("dmesg saved: %d bytes", n); } }
  time_t t0 = time(0); int keys = 0;
  while (time(0) - t0 < 30) {
    struct pollfd pf[8]; for (int i = 0; i < nf; i++) { pf[i].fd = fds[i]; pf[i].events = POLLIN; }
    if (nf == 0) { sleep(1); continue; } if (poll(pf, nf, 500) > 0) for (int i = 0; i < nf; i++) if (pf[i].revents & POLLIN) {
      struct input_event ev; while (read(fds[i], &ev, sizeof ev) == sizeof ev) if (ev.type == EV_KEY && ev.value == 1) {
        keys++; L("KEY code=%d (input%d)", ev.code, i);
        if (fbm) { rect(0, 0, 0, W, H, rgb((ev.code * 53) & 255, (ev.code * 97) & 255, (ev.code * 31) & 255)); rect(0, 60, 100, W - 60, 220, rgb(255, 255, 255)); present(0); }
      }
    }
  }
  L("done after 30 s, %d key presses; rebooting to normal", keys);
  sync(); if (lg) fclose(lg); sync(); umount("/cache");
  syscall(SYS_reboot, 0xfee1dead, 672274793, 0x01234567, 0);    // LINUX_REBOOT_CMD_RESTART: normal boot
  for (;;) sleep(1);
}
