package com.carrot.mods;

import com.carrot.CarrotScreen;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

/**
 * Ctrl+S in a world: shaders and resource packs in one place. Click a row to switch it,
 * drop a .zip on the window to add your own. Built on Carrot's own screen base.
 */
public final class LibraryScreen extends CarrotScreen {
    private static final int BG = 0xF0121317, EDGE = 0xFF2A2D35, ROW = 0xFF171A20, ROW_HOT = 0xFF222733;
    private static final int ACCENT = 0xFF8000FF, TEXT = 0xFFE8EAF0, DIM = 0xFF8A90A0, ON = 0xFF5CD66B;
    private static final int ROW_H = 24, ROW_GAP = 4, TOP = 44, FOOT = 40;

    private int tab;        // 0 shaders, 1 resource packs
    private int page;
    private String status = "Click a row to switch it. Drop a .zip here to add your own.";
    private List<String> items = List.of();
    private Set<String> active = Set.of();

    public LibraryScreen(Screen parent) {
        super("LIBRARY", parent);
    }

    @Override protected int panelW() { return Math.min(this.width - 40, 520); }
    @Override protected int panelH() { return Math.min(this.height - 40, 300); }
    @Override protected int panelX() { return (this.width - panelW()) / 2; }
    @Override protected int panelY() { return (this.height - panelH()) / 2; }

    private int perPage() { return Math.max(2, (panelH() - TOP - FOOT) / (ROW_H + ROW_GAP)); }
    private int footY() { return panelY() + panelH() - 30; }

    @Override
    protected void build() {
        Minecraft mc = Minecraft.getInstance();
        boolean shaders = tab == 0;
        items = Library.list(shaders ? Library.shaders() : Library.packs());
        active = shaders ? Set.of(Library.currentShader()) : Library.activePackFiles(mc);
        int pages = Math.max(1, (items.size() + perPage() - 1) / perPage());
        page = Math.min(page, pages - 1);

        int px = panelX(), py = panelY(), pw = panelW();
        hit(px + 12, py + 10, 92, 22, () -> switchTab(0));
        hit(px + 110, py + 10, 130, 22, () -> switchTab(1));

        for (int i = 0; i < perPage(); i++) {
            int idx = page * perPage() + i;
            if (idx >= items.size()) break;
            String name = items.get(idx);
            hit(px + 12, py + TOP + i * (ROW_H + ROW_GAP), pw - 24, ROW_H, () -> activate(name));
        }

        int fy = footY();
        hit(px + 12, fy, 100, 22, () -> Library.openFolder(tab == 0));
        hit(px + 118, fy, 64, 22, this::refresh);
        if (shaders) hit(px + 188, fy, 70, 22, () -> say(Library.applyShader("", false)));
        hit(px + pw - 72, fy, 60, 22, this::onClose);
        hit(px + pw - 104, fy, 28, 22, () -> turn(1));
        hit(px + pw - 136, fy, 28, 22, () -> turn(-1));
    }

    private void switchTab(int t) { tab = t; page = 0; rebuildWidgets(); }
    private void refresh() { say("Refreshed"); }
    private void turn(int d) { page = Math.max(0, page + d); rebuildWidgets(); }
    private void say(String s) { status = s; rebuildWidgets(); }

    private void activate(String name) {
        Minecraft mc = Minecraft.getInstance();
        say(tab == 0 ? Library.applyShader(name, !name.equals(Library.currentShader()))
                     : Library.togglePack(mc, name));
    }

    /** Drag and drop; picked up by name so a signature change just disables it. */
    public void onFilesDrop(List<Path> paths) {
        StringBuilder sb = new StringBuilder();
        for (Path p : paths) sb.append(Library.importFile(p)).append("  ");
        say(sb.toString().trim());
    }

    @Override
    protected void paint(GuiGraphicsExtractor g, int mx, int my) {
        int px = panelX(), py = panelY(), pw = panelW(), ph = panelH();
        rect(g, px, py, pw, ph, BG);
        rect(g, px, py, pw, 1, EDGE);
        rect(g, px, py + ph - 1, pw, 1, EDGE);
        rect(g, px, py, 1, ph, EDGE);
        rect(g, px + pw - 1, py, 1, ph, EDGE);
        rect(g, px, py, pw, 2, ACCENT);

        tabLabel(g, px + 12, py + 10, 92, "SHADERS", tab == 0, mx, my);
        tabLabel(g, px + 110, py + 10, 130, "RESOURCE PACKS", tab == 1, mx, my);

        if (items.isEmpty()) {
            g.text(this.font, "Nothing here yet. Drop a .zip onto this window.", px + 14, py + TOP + 8, DIM, false);
        }
        for (int i = 0; i < perPage(); i++) {
            int idx = page * perPage() + i;
            if (idx >= items.size()) break;
            String name = items.get(idx);
            int y = py + TOP + i * (ROW_H + ROW_GAP);
            boolean hot = inside(mx, my, px + 12, y, pw - 24, ROW_H);
            boolean on = active.contains(name);
            rect(g, px + 12, y, pw - 24, ROW_H, hot ? ROW_HOT : ROW);
            if (on) rect(g, px + 12, y, 3, ROW_H, ON);
            g.text(this.font, pretty(name), px + 22, y + 8, TEXT, true);
            String tag = on ? "ACTIVE" : hot ? (tab == 0 ? "CLICK TO USE" : "CLICK TO ENABLE") : "";
            g.text(this.font, tag, px + pw - 20 - this.font.width(tag), y + 8, on ? ON : DIM, false);
        }

        int fy = footY();
        g.text(this.font, status, px + 12, fy - 14, DIM, false);
        button(g, px + 12, fy, 100, "OPEN FOLDER", mx, my);
        button(g, px + 118, fy, 64, "REFRESH", mx, my);
        if (tab == 0) button(g, px + 188, fy, 70, "SHADERS OFF", mx, my);
        button(g, px + pw - 136, fy, 28, "<", mx, my);
        button(g, px + pw - 104, fy, 28, ">", mx, my);
        button(g, px + pw - 72, fy, 60, "CLOSE", mx, my);
    }

    private void tabLabel(GuiGraphicsExtractor g, int x, int y, int w, String label, boolean on, int mx, int my) {
        boolean hot = inside(mx, my, x, y, w, 22);
        g.text(this.font, label, x + 6, y + 7, on ? ACCENT : hot ? TEXT : DIM, true);
        if (on) rect(g, x, y + 20, w, 2, ACCENT);
    }

    private void button(GuiGraphicsExtractor g, int x, int y, int w, String label, int mx, int my) {
        boolean hot = inside(mx, my, x, y, w, 22);
        rect(g, x, y, w, 22, hot ? ROW_HOT : ROW);
        rect(g, x, y + 21, w, 1, hot ? ACCENT : EDGE);
        g.text(this.font, label, x + (w - this.font.width(label)) / 2, y + 7, hot ? TEXT : DIM, false);
    }

    private static String pretty(String file) {
        String n = file.endsWith(".zip") ? file.substring(0, file.length() - 4) : file;
        return n.replace('_', ' ').trim();
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static void rect(GuiGraphicsExtractor g, int x, int y, int w, int h, int argb) {
        g.fill(x, y, x + w, y + h, argb);
    }
}
