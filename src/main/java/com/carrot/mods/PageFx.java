package com.carrot.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

/**
 * Dark Carrot theme laid over every vanilla page (singleplayer, multiplayer, options, ...).
 * Menus get a deep tint, an orange glow and embers; in a world (inventory, pause) only a
 * dark tint and vignette so the inventory reads as a dark UI.
 */
public final class PageFx {
    private PageFx() {}

    public static void apply(Screen screen, GuiGraphicsExtractor g) {
        if (!Cfg.I.pageTheme) return;
        if (screen.getClass().getName().startsWith("com.carrot.")) return;   // Carrot screens draw their own
        draw(g, screen.width, screen.height, Minecraft.getInstance().level != null);
    }

    private static void draw(GuiGraphicsExtractor g, int w, int h, boolean inWorld) {
        if (inWorld) {
            g.fill(0, 0, w, h, 0x78000000);
            TitleFx.vignette(g, w, h);
            return;
        }
        double t = System.nanoTime() / 1.0e9;
        g.fill(0, 0, w, h, 0xA8000000);
        for (int i = 0; i < 30; i++) {
            int a = (int) (0x2E * (1.0 - i / 30.0));
            g.fill(0, i * 2, w, i * 2 + 2, (a << 24) | 0x8000FF);
        }
        TitleFx.embers(g, w, h, t, 26);
        TitleFx.sweep(g, w, t);
        TitleFx.vignette(g, w, h);

        Font font = Minecraft.getInstance().font;
        g.text(font, "OONA", 12, h - 16, 0x888000FF, false);
    }
}
