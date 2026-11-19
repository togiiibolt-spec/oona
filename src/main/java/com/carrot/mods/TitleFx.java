package com.carrot.mods;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared screen effects: edge vignette, rising embers, a sweeping edge glow. */
final class TitleFx {
    private TitleFx() {}

    static void vignette(GuiGraphicsExtractor g, int w, int h) {
        for (int i = 0; i < 10; i++) {
            int a = (int) (80 * (1.0 - i / 10.0));
            int c = a << 24;
            g.fill(0, i * 3, w, i * 3 + 3, c);
            g.fill(0, h - i * 3 - 3, w, h - i * 3, c);
            g.fill(i * 3, 0, i * 3 + 3, h, c);
            g.fill(w - i * 3 - 3, 0, w - i * 3, h, c);
        }
    }

    static void embers(GuiGraphicsExtractor g, int w, int h, double t, int count) {
        for (int i = 0; i < count; i++) {
            double seed = i * 12.9898;
            double speed = 14 + frac(seed * 3.1) * 26;
            double x = frac(seed * 7.7) * w + Math.sin(t * 0.8 + i) * 10;
            double y = h - ((t * speed + frac(seed * 5.3) * h) % h);
            int size = 1 + (i % 3 == 0 ? 1 : 0);
            double fade = Math.sin(Math.PI * (1.0 - y / h));
            int a = (int) (190 * Math.max(0, fade));
            int c = (a << 24) | (i % 2 == 0 ? 0x8000FF : 0xB66BFF);
            g.fill((int) x, (int) y, (int) x + size, (int) y + size, c);
        }
    }

    static void sweep(GuiGraphicsExtractor g, int w, double t) {
        int cx = (int) ((t * 0.35 % 1.0) * (w + 240)) - 120;
        for (int i = -120; i < 120; i += 4) {
            double k = 1.0 - Math.abs(i) / 120.0;
            int a = (int) (170 * k * k);
            g.fill(cx + i, 0, cx + i + 4, 2, (a << 24) | 0x8000FF);
        }
    }

    private static double frac(double v) {
        return v - Math.floor(v);
    }
}
