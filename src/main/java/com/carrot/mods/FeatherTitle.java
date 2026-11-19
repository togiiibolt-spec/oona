package com.carrot.mods;

import com.carrot.AccountsScreen;
import com.carrot.CosmeticsScreen;
import com.carrot.ModMenuScreen;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Centered logo, glass buttons with an accent call-to-action, icon strip and promo card,
 * over a parallax background. Replaces Carrot's own title screen.
 */
public final class FeatherTitle extends Screen {
    private static final Identifier BG = Identifier.fromNamespaceAndPath("carrotclient", "textures/gui/background.png");
    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("carrotclient", "textures/gui/logo.png");

    private static final int ACCENT = 0xFF8000FF, ACCENT_DARK = 0xFF4A00A8;
    private static final int GLASS = 0xC8101218, GLASS_HOT = 0xE61D2029;
    private static final int TEXT = 0xFFB9BEC9, TEXT_HOT = 0xFFFFFFFF, DIM = 0xFF6D7380;

    private static final int NONE = 0, GEAR = 1, GRID = 2, PERSON = 3, BOOKS = 4, CARD = 5, QUIT = 6;

    private record Btn(String label, int x, int y, int w, int h, boolean accent, int icon, Runnable run) {}

    private final long born = System.nanoTime();
    private final List<Btn> btns = new ArrayList<>();
    private float[] hover = new float[0];

    public FeatherTitle() {
        super(Component.literal("Oona"));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        btns.clear();

        int w = Math.min(190, this.width - 40);
        int x = (this.width - w) / 2;
        int y = (int) (this.height * 0.36);
        btns.add(new Btn("Singleplayer", x, y, w, 34, false, NONE, () -> mc.gui.setScreen(new SelectWorldScreen(this))));
        btns.add(new Btn("Multiplayer", x, y + 40, w, 34, false, NONE, () -> mc.gui.setScreen(new JoinMultiplayerScreen(this))));
        btns.add(new Btn("Cosmetics", x, y + 80, w, 34, false, NONE, () -> mc.gui.setScreen(new CosmeticsScreen(this))));
        btns.add(new Btn("Shader Library", x, y + 120, w, 34, true, NONE, () -> mc.gui.setScreen(new LibraryScreen(this))));
        btns.add(new Btn("QUIT GAME", (this.width - 150) / 2, this.height - 74, 150, 28, false, QUIT, mc::stop));

        int ix = this.width - 40;
        btns.add(new Btn("Settings", ix, 12, 28, 28, false, GEAR,
            () -> mc.gui.setScreen(new OptionsScreen(this, mc.options, false))));
        btns.add(new Btn("Library", ix - 34, 12, 28, 28, false, BOOKS, () -> mc.gui.setScreen(new LibraryScreen(this))));
        btns.add(new Btn("Mods", ix - 68, 12, 28, 28, false, GRID, () -> mc.gui.setScreen(new ModMenuScreen(this))));
        btns.add(new Btn("Accounts", ix - 102, 12, 28, 28, false, PERSON, () -> mc.gui.setScreen(new AccountsScreen(this))));
        btns.add(new Btn("", this.width - 162, this.height - 98, 150, 86, false, CARD,
            () -> mc.gui.setScreen(new ModMenuScreen(this))));

        hover = new float[btns.size()];
        for (Btn b : btns) {
            Runnable run = b.run();
            addRenderableWidget(Button.builder(Component.empty(), button -> run.run())
                .bounds(b.x(), b.y(), b.w(), b.h()).build());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int w = this.width, h = this.height;
        double t = (System.nanoTime() - born) / 1.0e9;
        float intro = ease((float) Math.min(1.0, t / 0.7));

        // parallax background: the whole image is stretched over a quad slightly bigger than the screen
        int ox = (int) ((mx / (float) w - 0.5f) * -24f) - 24;
        int oy = (int) ((my / (float) h - 0.5f) * -14f) - 14;
        g.blit(RenderPipelines.GUI_TEXTURED, BG, ox, oy, 0f, 0f, w + 48, h + 28, w + 48, h + 28);
        g.fill(0, 0, w, h, 0xC4000000);
        TitleFx.embers(g, w, h, t, 30);
        TitleFx.vignette(g, w, h);

        drawLogo(g, font, w, h, t);

        for (int i = 0; i < btns.size(); i++) {
            Btn b = btns.get(i);
            int slide = (int) ((1f - intro) * (10 + i * 2));
            boolean hot = mx >= b.x() && mx < b.x() + b.w() && my >= b.y() + slide && my < b.y() + slide + b.h();
            hover[i] += ((hot ? 1f : 0f) - hover[i]) * 0.25f;
            drawButton(g, font, b, b.y() + slide, hover[i]);
        }
        drawAccountChip(g, font, w);
        drawTooltip(g, font, mx, my);

        g.text(font, "Oona 26.2", (w - font.width("Oona 26.2")) / 2, h - 30, DIM, false);
        TitleFx.sweep(g, w, t);

        int fade = (int) ((1f - intro) * 255f);
        if (fade > 0) g.fill(0, 0, w, h, fade << 24);
    }

    // ---- pieces ----

    private void drawLogo(GuiGraphicsExtractor g, Font font, int w, int h, double t) {
        String name = "OONA";
        int size = 52;
        int total = size + 12 + font.width(name);
        int x = (w - total) / 2;
        int y = (int) (h * 0.12) + (int) Math.round(Math.sin(t * 1.4) * 2);
        g.blit(RenderPipelines.GUI_TEXTURED, LOGO, x, y, 0f, 0f, size, size, size, size);
        int tx = x + size + 12, ty = y + size / 2 - 4;
        g.text(font, name, tx, ty, 0xFFFFFFFF, true);
        g.text(font, name, tx + 1, ty, 0xFFFFFFFF, false);   // faux bold
        g.text(font, "PvP  -  Shaders  -  Library", tx, ty + 13, ACCENT, false);
    }

    private void drawButton(GuiGraphicsExtractor g, Font font, Btn b, int y, float hot) {
        int x = b.x(), bw = b.w(), bh = b.h();
        switch (b.icon()) {
            case CARD -> { drawCard(g, font, x, y, bw, bh, hot); return; }
            case QUIT -> {
                g.fill(x, y, x + bw, y + bh, lerp(0x308000FF, 0x608000FF, hot));
                center(g, font, b.label(), x + bw / 2, y + (bh - 8) / 2, lerp(ACCENT, TEXT_HOT, hot));
                return;
            }
            default -> { }
        }
        if (b.accent()) {
            for (int r = 0; r < bh; r++) {
                int c = lerp(ACCENT, ACCENT_DARK, r / (float) bh);
                g.fill(x, y + r, x + bw, y + r + 1, lerp(c, 0xFFC79BFF, hot * 0.35f));
            }
            g.fill(x, y, x + bw, y + 1, 0x66FFFFFF);
        } else {
            g.fill(x, y, x + bw, y + bh, lerp(GLASS, GLASS_HOT, hot));
            int edge = lerp(0x22FFFFFF, 0x998000FF, hot);
            g.fill(x, y, x + bw, y + 1, edge);
            g.fill(x, y + bh - 1, x + bw, y + bh, edge);
            g.fill(x, y, x + 1, y + bh, edge);
            g.fill(x + bw - 1, y, x + bw, y + bh, edge);
        }
        int fg = b.accent() ? 0xFFFFFFFF : lerp(TEXT, TEXT_HOT, hot);
        if (b.icon() != NONE) icon(g, b.icon(), x + bw / 2, y + bh / 2, fg);
        else center(g, font, b.label(), x + bw / 2, y + (bh - 8) / 2, fg);
    }

    private void drawCard(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, float hot) {
        int lift = (int) (hot * -3);
        y += lift;
        g.fill(x, y, x + w, y + h, 0xE0101218);
        for (int r = 0; r < 24; r++) g.fill(x, y + r, x + w, y + r + 1, ((int) (0x40 * (1 - r / 24f)) << 24) | 0x8000FF);
        g.fill(x, y, x + w, y + 2, ACCENT);
        g.text(font, "WHAT'S NEW", x + 10, y + 10, ACCENT, false);
        g.text(font, "Variety, Auto Pots,", x + 10, y + 28, TEXT_HOT, false);
        g.text(font, "BedWars Assist", x + 10, y + 40, TEXT_HOT, false);
        g.text(font, "Shaders: Ctrl+S in game", x + 10, y + 60, DIM, false);
        g.text(font, "or the Library button", x + 10, y + 71, DIM, false);
    }

    private void drawAccountChip(GuiGraphicsExtractor g, Font font, int w) {
        String name = playerName();
        int cw = 28 + font.width(name) + 14;
        int x = w - 40 - 102 - 12 - cw, y = 12;
        g.fill(x, y, x + cw, y + 28, GLASS);
        g.fill(x + 4, y + 4, x + 24, y + 24, ACCENT);
        g.text(font, name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(), x + 12, y + 10, 0xFFFFFFFF, false);
        g.text(font, name, x + 30, y + 10, TEXT_HOT, false);
    }

    private void drawTooltip(GuiGraphicsExtractor g, Font font, int mx, int my) {
        for (Btn b : btns) {
            if (b.icon() == NONE || b.icon() == CARD || b.icon() == QUIT) continue;
            if (mx >= b.x() && mx < b.x() + b.w() && my >= b.y() && my < b.y() + b.h()) {
                int tw = font.width(b.label());
                int x = Math.min(b.x() + b.w() - tw - 8, this.width - tw - 12);
                g.fill(x, b.y() + b.h() + 4, x + tw + 8, b.y() + b.h() + 18, 0xF0101218);
                g.text(font, b.label(), x + 4, b.y() + b.h() + 7, TEXT_HOT, false);
            }
        }
    }

    /** Pixel pictograms; no textures needed. */
    private static void icon(GuiGraphicsExtractor g, int kind, int cx, int cy, int c) {
        switch (kind) {
            case GEAR -> {
                box(g, cx - 5, cy - 5, 10, 10, c);
                box(g, cx - 2, cy - 2, 4, 4, GLASS_HOT);
                box(g, cx - 1, cy - 8, 2, 3, c);
                box(g, cx - 1, cy + 5, 2, 3, c);
                box(g, cx - 8, cy - 1, 3, 2, c);
                box(g, cx + 5, cy - 1, 3, 2, c);
                box(g, cx - 6, cy - 6, 2, 2, c);
                box(g, cx + 4, cy - 6, 2, 2, c);
                box(g, cx - 6, cy + 4, 2, 2, c);
                box(g, cx + 4, cy + 4, 2, 2, c);
            }
            case GRID -> {
                box(g, cx - 6, cy - 6, 5, 5, c);
                box(g, cx + 1, cy - 6, 5, 5, c);
                box(g, cx - 6, cy + 1, 5, 5, c);
                box(g, cx + 1, cy + 1, 5, 5, c);
            }
            case PERSON -> {
                box(g, cx - 3, cy - 8, 6, 6, c);
                box(g, cx - 6, cy - 1, 12, 7, c);
            }
            case BOOKS -> {
                box(g, cx - 7, cy - 6, 4, 12, c);
                box(g, cx - 2, cy - 8, 4, 14, c);
                box(g, cx + 3, cy - 5, 4, 11, c);
            }
            default -> { }
        }
    }

    // ---- helpers ----

    private static String playerName() {
        Minecraft mc = Minecraft.getInstance();
        Object user = Compat.get(mc, "getUser");
        Object name = Compat.get(user, "getName");
        return name == null ? "Player" : String.valueOf(name);
    }

    private static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + h, c);
    }

    private static void center(GuiGraphicsExtractor g, Font font, String s, int cx, int y, int c) {
        g.text(font, s, cx - font.width(s) / 2, y, c, true);
    }

    private static float ease(float t) {
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 255, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = (b >>> 24) & 255, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
            | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }
}
