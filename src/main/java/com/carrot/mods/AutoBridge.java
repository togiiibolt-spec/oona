package com.carrot.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Places a block under your feet (or just ahead of them) while you walk off an edge. */
final class AutoBridge {
    private static final Direction[] SUPPORT_ORDER = {
        Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP};
    private static int cooldown;

    private AutoBridge() {}

    static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        Level level = mc.level;
        if (p == null || level == null || mc.gameMode == null || mc.gui.screen() != null) return;
        if (cooldown > 0) { cooldown--; return; }
        if (!p.onGround()) return;

        BlockPos below = p.blockPosition().below();
        Vec3 v = p.getDeltaMovement();
        BlockPos ahead = BlockPos.containing(
            p.getX() + v.x * Cfg.I.bridgeLead, p.getY() - 1.0, p.getZ() + v.z * Cfg.I.bridgeLead);

        BlockPos target = null;
        if (level.getBlockState(below).canBeReplaced()) target = below;
        else if (!ahead.equals(below) && level.getBlockState(ahead).canBeReplaced()) target = ahead;
        if (target == null) return;

        BlockHitResult hit = findSupport(level, target);
        if (hit == null) return;

        if (!placeable(p.getMainHandItem())) {
            if (!Cfg.I.bridgeAutoSelect) return;
            int slot = -1;
            for (int i = 0; i < 9; i++) {
                if (placeable(p.getInventory().getItem(i))) { slot = i; break; }
            }
            if (slot < 0) return;
            Compat.selectHotbar(p.getInventory(), slot);
        }

        InteractionResult r = mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, hit);
        if (r.consumesAction()) p.swing(InteractionHand.MAIN_HAND);
        cooldown = Math.max(0, Cfg.I.bridgeDelay);
    }

    private static BlockHitResult findSupport(Level level, BlockPos target) {
        for (Direction d : SUPPORT_ORDER) {
            BlockPos n = target.relative(d);
            BlockState s = level.getBlockState(n);
            if (s.isAir() || s.canBeReplaced()) continue;
            Direction face = d.getOpposite();
            Vec3 at = Vec3.atCenterOf(n).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
            return new BlockHitResult(at, face, n, false);
        }
        return null;
    }

    private static boolean placeable(ItemStack st) {
        if (st.isEmpty() || !(st.getItem() instanceof BlockItem bi)) return false;
        var b = bi.getBlock();
        return !(b instanceof EntityBlock) && !(b instanceof FallingBlock) && b != Blocks.TNT;
    }
}
