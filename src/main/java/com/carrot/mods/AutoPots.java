package com.carrot.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Low health: throws a healing splash potion at your feet, then puts the hotbar back. */
final class AutoPots {
    private static int cooldown;
    private static float savedPitch;
    private static boolean pending;

    private AutoPots() {}

    static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        MultiPlayerGameMode gm = mc.gameMode;
        if (p == null || gm == null) return;

        // the throw needs the pitch down for one tick; give it back afterwards
        if (pending) { p.setXRot(savedPitch); pending = false; }
        if (cooldown > 0) { cooldown--; return; }
        if (mc.gui.screen() != null || p.containerMenu != p.inventoryMenu || !p.isAlive()) return;
        if (p.getHealth() > Cfg.I.potHealth) return;

        int slot = findPotion(p);
        if (slot < 0) return;

        Inventory inv = p.getInventory();
        int selected = Compat.selected(inv);
        int restoreSelected = -1;
        int swapMenu = -1;
        if (slot < 9) {
            if (slot != selected) { restoreSelected = selected; Compat.selectHotbar(inv, slot); }
        } else {
            if (!Compat.clickSwap(mc, p, slot, selected)) return;
            swapMenu = slot;
        }

        savedPitch = p.getXRot();
        p.setXRot(90f);
        InteractionResult r = gm.useItem(p, InteractionHand.MAIN_HAND);
        if (r.consumesAction()) p.swing(InteractionHand.MAIN_HAND);
        pending = true;

        if (swapMenu >= 0) Compat.clickSwap(mc, p, swapMenu, selected);
        if (restoreSelected >= 0) Compat.selectHotbar(inv, restoreSelected);
        cooldown = Math.max(1, Cfg.I.potDelay);
    }

    private static int findPotion(LocalPlayer p) {
        for (int i = 0; i < 36; i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (!st.is(Items.SPLASH_POTION)) continue;
            String name = st.getHoverName().getString();
            for (String m : Cfg.I.potMatch) {
                if (name.contains(m)) return i;
            }
        }
        return -1;
    }
}
