package com.carrot.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** What xray adds on top of hiding blocks: fullbright, sparkles on every ore, and a radar line. */
final class XrayFx {
    private static final int RADIUS = 12;
    private static final int MAX_SPARKS = 48;
    private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private static int ticks;
    private static boolean gaveNightVision;

    private XrayFx() {}

    static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        Level level = mc.level;
        if (p == null || level == null) { gaveNightVision = false; return; }

        if (!Xray.enabled) {
            if (gaveNightVision) { p.removeEffect(MobEffects.NIGHT_VISION); gaveNightVision = false; }
            return;
        }
        if (Cfg.I.xrayFullbright && !gaveNightVision && !p.hasEffect(MobEffects.NIGHT_VISION)) {
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1_000_000, 0, false, false, false));
            gaveNightVision = true;
        }
        if (++ticks % 10 != 0) return;

        BlockPos centre = p.blockPosition();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int sparks = 0;
        double bestDist = Double.MAX_VALUE;
        BlockPos best = null;
        BlockState bestState = null;
        boolean bestIsDiamond = false;

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    m.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    BlockState s = level.getBlockState(m);
                    if (s.isAir() || !Xray.isTarget(s)) continue;

                    if (Cfg.I.xrayGlow && sparks < MAX_SPARKS) {
                        level.addParticle(ParticleTypes.END_ROD, m.getX() + 0.5, m.getY() + 0.5, m.getZ() + 0.5, 0, 0, 0);
                        sparks++;
                    }
                    double d = dx * dx + dy * dy + dz * dz;
                    boolean diamond = BlockNames.path(s).contains("diamond");
                    // diamonds outrank everything else, then plain distance
                    if ((diamond && !bestIsDiamond) || (diamond == bestIsDiamond && d < bestDist)) {
                        bestDist = d;
                        best = m.immutable();
                        bestState = s;
                        bestIsDiamond = diamond;
                    }
                }
            }
        }

        if (Cfg.I.xrayRadar && best != null && ticks % 40 == 0) {
            int dx = best.getX() - centre.getX();
            int dy = best.getY() - centre.getY();
            int dz = best.getZ() - centre.getZ();
            int idx = ((int) Math.round(Math.toDegrees(Math.atan2(dx, -dz)) / 45.0) + 8) % 8;
            String height = dy > 2 ? ", up " + dy : dy < -2 ? ", down " + (-dy) : "";
            Compat.overlay(mc, "Xray: " + bestState.getBlock().getName().getString()
                + " " + (int) Math.sqrt(bestDist) + "m " + COMPASS[idx] + height);
        }
    }
}
