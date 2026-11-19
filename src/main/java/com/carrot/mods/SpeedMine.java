package com.carrot.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Client break-speed multiplier plus removal of the delay between blocks. */
final class SpeedMine {
    private static boolean applied;
    private static double orig = 1.0;
    private static LocalPlayer last;

    private SpeedMine() {}

    static void tick(Minecraft mc, boolean want) {
        LocalPlayer p = mc.player;
        if (p == null) { applied = false; last = null; return; }
        if (p != last) { last = p; applied = false; }

        AttributeInstance speed = p.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        if (speed != null) {
            if (want) {
                if (!applied) { orig = speed.getBaseValue(); applied = true; }
                speed.setBaseValue(Math.max(1.0, Cfg.I.mineSpeed));
            } else if (applied) {
                speed.setBaseValue(orig);
                applied = false;
            }
        }
        if (want && Cfg.I.mineNoDelay && mc.options.keyAttack.isDown()) {
            Compat.setInt(mc.gameMode, "destroyDelay", 0);
        }
    }
}
