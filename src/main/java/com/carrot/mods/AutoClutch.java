package com.carrot.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Falling far enough to get hurt: pulls a clutch item (Cfg.clutchItems, in order) from the
 * inventory into the main hand, places it on the ground under you, then scoops the water
 * back up and puts your hotbar the way it was.
 */
final class AutoClutch {
    private static final int SCOOP_TIMEOUT = 50;
    private static final int REST = 20;

    private static int phase;          // 0 idle, 1 water down / waiting to land
    private static int timer, cooldown;
    private static int restoreSlot = -1, swapBackMenu = -1, swapBackButton = -1;
    private static BlockPos water;
    private static double lastY, fell;
    private static boolean haveLast;

    private AutoClutch() {}

    static void reset() {
        phase = 0;
        fell = 0;
        haveLast = false;
    }

    static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        Level level = mc.level;
        MultiPlayerGameMode gm = mc.gameMode;
        if (p == null || level == null || gm == null) return;

        track(p);
        if (cooldown > 0) cooldown--;
        if (phase == 1) { scoop(mc, p, level, gm); return; }
        if (cooldown > 0 || fell < Cfg.I.clutchMinFall) return;
        if (p.isFallFlying() || p.isInWater() || p.onClimbable() || p.isSpectator()
            || p.getAbilities().flying || p.getAbilities().mayfly) return;

        BlockPos ground = findGround(level, p);
        if (ground == null) return;
        double surface = ground.getY() + 1.0;
        double eyeDistance = p.getY() + p.getEyeHeight() - surface;
        if (eyeDistance > Cfg.I.clutchReach) return;

        int slot = findClutchItem(p, level);
        if (slot < 0) return;
        if (!hold(mc, p, slot)) return;

        ItemStack held = p.getMainHandItem();
        if (held.is(Items.WATER_BUCKET)) {
            // buckets use the look ray, so face straight down first
            p.setXRot(90f);
            InteractionResult r = gm.useItem(p, InteractionHand.MAIN_HAND);
            if (r.consumesAction()) {
                p.swing(InteractionHand.MAIN_HAND);
                water = ground.above();
                phase = 1;
                timer = 0;
                return;
            }
        } else {
            Vec3 top = Vec3.atCenterOf(ground).add(0.0, 0.5, 0.0);
            InteractionResult r = gm.useItemOn(p, InteractionHand.MAIN_HAND, new BlockHitResult(top, Direction.UP, ground, false));
            if (r.consumesAction()) p.swing(InteractionHand.MAIN_HAND);
        }
        finish(mc, p);
    }

    private static void scoop(Minecraft mc, LocalPlayer p, Level level, MultiPlayerGameMode gm) {
        timer++;
        boolean landed = p.isInWater() || p.onGround();
        if (!landed && timer < SCOOP_TIMEOUT) return;

        if (Cfg.I.clutchPickup && landed && water != null && !level.getFluidState(water).isEmpty()) {
            if (!p.getMainHandItem().is(Items.BUCKET)) {
                int bucket = findItem(p, "bucket");
                if (bucket >= 0) hold(mc, p, bucket);
            }
            if (p.getMainHandItem().is(Items.BUCKET)) {
                float[] rot = Aimbot.rotationTo(p.getEyePosition(), Vec3.atCenterOf(water));
                p.setYRot(rot[0]);
                p.setXRot(rot[1]);
                if (gm.useItem(p, InteractionHand.MAIN_HAND).consumesAction()) p.swing(InteractionHand.MAIN_HAND);
            }
        }
        finish(mc, p);
    }

    /** Puts item inventory index {@code slot} into the main hand; remembers how to undo it. */
    private static boolean hold(Minecraft mc, LocalPlayer p, int slot) {
        Inventory inv = p.getInventory();
        int selected = Compat.selected(inv);
        if (slot < 9) {
            if (slot != selected) {
                if (restoreSlot < 0) restoreSlot = selected;
                Compat.selectHotbar(inv, slot);
            }
            return true;
        }
        if (swapBackMenu >= 0) return true;   // already swapped once this clutch
        if (!Compat.clickSwap(mc, p, slot, selected)) return false;
        swapBackMenu = slot;
        swapBackButton = selected;
        return true;
    }

    private static void finish(Minecraft mc, LocalPlayer p) {
        if (swapBackMenu >= 0) Compat.clickSwap(mc, p, swapBackMenu, swapBackButton);
        if (restoreSlot >= 0) Compat.selectHotbar(p.getInventory(), restoreSlot);
        swapBackMenu = -1;
        swapBackButton = -1;
        restoreSlot = -1;
        water = null;
        phase = 0;
        fell = 0;
        cooldown = REST;
    }

    private static void track(LocalPlayer p) {
        double y = p.getY();
        if (p.onGround() || p.isInWater() || p.isFallFlying()) fell = 0;
        else if (haveLast && y > lastY) fell = 0;
        else if (haveLast) fell += lastY - y;
        lastY = y;
        haveLast = true;
    }

    /** First solid block under the player within 8 blocks, or null (void, or water already below). */
    private static BlockPos findGround(Level level, LocalPlayer p) {
        BlockPos base = BlockPos.containing(p.getX(), p.getY(), p.getZ());
        for (int dy = 1; dy <= 8; dy++) {
            BlockPos pos = base.below(dy);
            BlockState s = level.getBlockState(pos);
            if (!s.getFluidState().isEmpty()) return null;
            if (!s.getCollisionShape(level, pos).isEmpty()) return pos;
        }
        return null;
    }

    private static int findClutchItem(LocalPlayer p, Level level) {
        boolean nether = level.dimension() == Level.NETHER;
        for (String id : Cfg.I.clutchItems) {
            if (nether && id.equals("water_bucket")) continue;   // water evaporates there
            int slot = findItem(p, id);
            if (slot >= 0) return slot;
        }
        return -1;
    }

    private static int findItem(LocalPlayer p, String idPath) {
        for (int i = 0; i < 36; i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (!st.isEmpty() && BuiltInRegistries.ITEM.getKey(st.getItem()).getPath().equals(idPath)) return i;
        }
        return -1;
    }
}
