package com.carrot.mods;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Aimbot (snap) and Aim Control (eased, wobbling, sticky target) share this code.
 * control=false: close the whole angle up to aimSpeed per tick.
 * control=true:  close aimSmooth of the angle, add jitter, keep the locked target.
 */
final class Aimbot {
    private static Entity locked;

    private Aimbot() {}

    static void tick(Minecraft mc, boolean control) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.gui.screen() != null) { locked = null; return; }
        if (Cfg.I.aimOnlyWhileAttack && !mc.options.keyAttack.isDown()) { locked = null; return; }

        Vec3 eye = p.getEyePosition();
        double range = Cfg.I.aimRange;

        Entity target = null;
        Vec3 point = null;
        if (control && Cfg.I.aimLockTarget && locked != null && usable(p, locked, eye, range)) {
            target = locked;
            point = aimPoint(locked);
        }
        if (target == null) {
            AABB box = p.getBoundingBox().inflate(range);
            double bestScore = Double.MAX_VALUE;
            for (Entity e : mc.level.getEntities(p, box, x -> valid(p, x))) {
                Vec3 pt = aimPoint(e);
                double dist = eye.distanceTo(pt);
                if (dist > range) continue;
                float[] rot = rotationTo(eye, pt);
                double angle = Math.hypot(Mth.wrapDegrees(rot[0] - p.getYRot()), rot[1] - p.getXRot());
                if (angle > Cfg.I.aimFov / 2.0) continue;
                if (Cfg.I.aimLineOfSight && !p.hasLineOfSight(e)) continue;
                double score = switch (Cfg.I.aimPriority) {
                    case "distance" -> dist;
                    case "health" -> ((LivingEntity) e).getHealth();
                    default -> angle;
                };
                if (score < bestScore) { bestScore = score; target = e; point = pt; }
            }
        }
        if (target == null) { locked = null; return; }
        locked = target;

        float[] rot = rotationTo(eye, point);
        float max = Math.max(1f, Cfg.I.aimSpeed);
        float ease = control ? Mth.clamp(Cfg.I.aimSmooth, 0.05f, 1f) : 1f;
        float dYaw = Mth.wrapDegrees(rot[0] - p.getYRot()) * ease;
        float dPitch = (rot[1] - p.getXRot()) * ease;
        if (control && Cfg.I.aimJitter > 0f && Math.abs(dYaw) + Math.abs(dPitch) > 0.5f) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            dYaw += (r.nextFloat() * 2f - 1f) * Cfg.I.aimJitter;
            dPitch += (r.nextFloat() * 2f - 1f) * Cfg.I.aimJitter;
        }
        dYaw = Mth.clamp(dYaw, -max, max);
        dPitch = Mth.clamp(dPitch, -max, max);
        p.setYRot(p.getYRot() + dYaw);
        p.setXRot(Mth.clamp(p.getXRot() + dPitch, -90f, 90f));
    }

    private static Vec3 aimPoint(Entity e) {
        return Cfg.I.aimHead ? e.getEyePosition() : e.getBoundingBox().getCenter();
    }

    private static boolean usable(LocalPlayer p, Entity e, Vec3 eye, double range) {
        if (!valid(p, e) || eye.distanceTo(aimPoint(e)) > range) return false;
        return !Cfg.I.aimLineOfSight || p.hasLineOfSight(e);
    }

    private static boolean valid(LocalPlayer self, Entity e) {
        if (e == self || !(e instanceof LivingEntity le) || !le.isAlive() || le.isSpectator()) return false;
        if (e instanceof ArmorStand) return false;
        if (e instanceof Player pl) return Cfg.I.aimPlayers && !pl.isCreative();
        return Cfg.I.aimMobs && e instanceof Enemy;
    }

    /** yaw 0 faces +Z, pitch positive looks down. */
    static float[] rotationTo(Vec3 from, Vec3 to) {
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        return new float[] {yaw, pitch};
    }
}
