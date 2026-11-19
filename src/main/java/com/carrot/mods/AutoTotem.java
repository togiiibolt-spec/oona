package com.carrot.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;

final class AutoTotem {
    private static final int OFFHAND_BUTTON = 40;
    private static int cooldown;

    private AutoTotem() {}

    static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.gameMode == null) return;
        if (cooldown > 0) { cooldown--; return; }
        // a container screen shifts slot ids; wait until the plain inventory menu is active
        if (p.containerMenu != p.inventoryMenu) return;
        if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
        if (p.getHealth() > Cfg.I.totemHealth) return;

        int slot = -1;
        for (int i = 0; i < 36; i++) {
            if (p.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) { slot = i; break; }
        }
        if (slot < 0) return;

        // inventory menu ids: 9-35 main, 36-44 hotbar
        int menuSlot = slot < 9 ? slot + 36 : slot;
        if (Compat.clickSwap(mc, p, menuSlot, OFFHAND_BUTTON)) cooldown = Math.max(0, Cfg.I.totemDelay);
    }
}
