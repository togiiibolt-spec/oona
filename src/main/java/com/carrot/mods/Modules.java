package com.carrot.mods;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** One row per toggle: drives the keybinds and the Carrot mod menu entries. */
final class Modules {
    record Entry(String id, String name, String glyph, String cat, int key,
                 BooleanSupplier get, Consumer<Boolean> set) {}

    static final int NONE = GLFW.GLFW_KEY_UNKNOWN;

    static final List<Entry> ALL = List.of(
        new Entry("variety", "Variety", "VARIETY", "PvP", GLFW.GLFW_KEY_V,
            () -> Cfg.I.variety, v -> Cfg.I.variety = v),
        new Entry("pvpgod", "PvP God", "GOD", "PvP", GLFW.GLFW_KEY_U,
            () -> Cfg.I.pvpGod, v -> Cfg.I.pvpGod = v),
        new Entry("aimbot", "Aimbot", "AIM", "PvP", GLFW.GLFW_KEY_H,
            () -> Cfg.I.aimbot, v -> Cfg.I.aimbot = v),
        new Entry("aimcontrol", "Aim Control", "CTRL", "PvP", NONE,
            () -> Cfg.I.aimControl, v -> Cfg.I.aimControl = v),
        new Entry("highdamage", "High Damage", "DMG", "PvP", NONE,
            () -> Cfg.I.highDamage, v -> Cfg.I.highDamage = v),
        new Entry("reach", "Reach", "REACH", "PvP", NONE,
            () -> Cfg.I.reach, v -> Cfg.I.reach = v),
        new Entry("autopots", "Auto Pots", "POTS", "PvP", NONE,
            () -> Cfg.I.autoPots, v -> Cfg.I.autoPots = v),
        new Entry("autototem", "Auto Totem", "TOTEM", "PvP", GLFW.GLFW_KEY_G,
            () -> Cfg.I.autoTotem, v -> Cfg.I.autoTotem = v),
        new Entry("autobridge", "Auto Bridge", "BRIDGE", "PvP", GLFW.GLFW_KEY_K,
            () -> Cfg.I.autoBridge, v -> Cfg.I.autoBridge = v),
        new Entry("autoclutch", "Auto Clutch", "CLUTCH", "PvP", NONE,
            () -> Cfg.I.autoClutch, v -> Cfg.I.autoClutch = v),
        new Entry("bedwars", "BedWars Assist", "BED", "PvP", NONE,
            () -> Cfg.I.bedWars, v -> Cfg.I.bedWars = v),
        new Entry("speedmine", "Speed Mine", "MINE", "Util", NONE,
            () -> Cfg.I.speedMine, v -> Cfg.I.speedMine = v),
        new Entry("xray", "Xray", "XRAY", "Util", GLFW.GLFW_KEY_J,
            () -> Xray.enabled,
            v -> { if (Xray.enabled != v) Xray.toggle(Minecraft.getInstance()); })
    );

    private Modules() {}
}
