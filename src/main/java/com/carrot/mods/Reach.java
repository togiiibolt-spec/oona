package com.carrot.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Raises the interaction-range attributes on the client; the server keeps its own limit. */
final class Reach {
    private static boolean applied;
    private static double origEntity, origBlock;
    private static LocalPlayer last;

    private Reach() {}

    static void tick(Minecraft mc, boolean want) {
        LocalPlayer p = mc.player;
        if (p == null) { applied = false; last = null; return; }
        if (p != last) { last = p; applied = false; }   // fresh player object = fresh attributes

        AttributeInstance entity = p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        AttributeInstance block = p.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (entity == null || block == null) return;

        if (want) {
            if (!applied) {
                origEntity = entity.getBaseValue();
                origBlock = block.getBaseValue();
                applied = true;
            }
            entity.setBaseValue(Cfg.I.reachEntity);
            block.setBaseValue(Cfg.I.reachBlock);
        } else if (applied) {
            entity.setBaseValue(origEntity);
            block.setBaseValue(origBlock);
            applied = false;
        }
    }
}
