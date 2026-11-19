package com.carrot.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** BedWars coach: one suggestion on the action bar every five seconds, from your inventory and the players near you. */
final class BedWars {
    private static final int EVERY = 100;
    private static int ticks;
    private static int tip;

    private BedWars() {}

    static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        if (++ticks % EVERY != 0) return;
        Compat.overlay(mc, "BedWars: " + suggest(mc, p));
    }

    private static String suggest(Minecraft mc, LocalPlayer p) {
        if (p.getHealth() <= 8f) return "low health, heal up or fall back to your base";

        for (Player o : mc.level.players()) {
            if (o == p || o.isSpectator()) continue;
            float d = p.distanceTo(o);
            if (d < 8f) return o.getName().getString() + " is " + (int) d + "m away, hit and strafe, keep blocks ready";
        }

        int blocks = 0, iron = 0, gold = 0, diamond = 0, emerald = 0, swordRank = -1;
        boolean apple = false;
        for (int i = 0; i < 36; i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(st.getItem()).getPath();
            int n = st.getCount();
            if (id.endsWith("_wool") || id.contains("terracotta") || id.contains("planks")
                || id.equals("end_stone") || id.equals("obsidian") || id.contains("glass")) blocks += n;
            switch (id) {
                case "iron_ingot" -> iron += n;
                case "gold_ingot" -> gold += n;
                case "diamond" -> diamond += n;
                case "emerald" -> emerald += n;
                case "golden_apple" -> apple = true;
                default -> { }
            }
            if (id.endsWith("_sword")) swordRank = Math.max(swordRank, rank(id));
        }

        if (blocks < 24) return "you have " + blocks + " blocks, buy wool or end stone";
        if (p.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) return "no chestplate, buy armor";
        if (swordRank < 1 && iron >= 10) return "10 iron buys a stone sword";
        if (swordRank < 2 && gold >= 7) return "7 gold buys an iron sword";
        if (diamond >= 3) return "3 diamonds, buy a team upgrade (sharpness or protection)";
        if (emerald >= 4) return "4 emeralds buys an ender pearl";
        if (!apple && gold >= 3) return "3 gold buys a golden apple";
        if (iron >= 24) return iron + " iron, buy a pickaxe or more blocks";

        String[] idle = {
            "cover your bed with wool, then end stone, then obsidian",
            "guard the bridge to your island, one block gap stops rushers",
            "watch the generators, a quick loop gets you resources first",
            "break the enemy bed before you go for kills"};
        return idle[tip++ % idle.length];
    }

    private static int rank(String id) {
        if (id.startsWith("netherite")) return 4;
        if (id.startsWith("diamond")) return 3;
        if (id.startsWith("iron")) return 2;
        if (id.startsWith("stone")) return 1;
        return 0;
    }
}
