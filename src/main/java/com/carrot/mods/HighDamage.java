package com.carrot.mods;

import java.lang.reflect.Constructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Critical hits on demand: right before an attack, a short hop of position packets
 * gives the server a non-zero fall distance, so the hit lands as a crit (x1.5).
 * The server only grants the crit while you are not sprinting.
 */
final class HighDamage {
    private static final double[] HOP = {0.0625, 0.0, 1.1E-5, 0.0};
    private static Constructor<?> ctor;
    private static boolean withCollisionFlag;
    private static boolean failed;

    private HighDamage() {}

    static void init() {
        try {
            Class<?> pos = Class.forName("net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$Pos");
            for (Constructor<?> k : pos.getConstructors()) {
                Class<?>[] t = k.getParameterTypes();
                boolean head = t.length >= 4 && t[0] == double.class && t[1] == double.class
                    && t[2] == double.class && t[3] == boolean.class;
                if (head && t.length == 4) { ctor = k; withCollisionFlag = false; break; }
                if (head && t.length == 5 && t[4] == boolean.class) { ctor = k; withCollisionFlag = true; break; }
            }
        } catch (Throwable ignored) {
            // handled by the null check below
        }
        if (ctor == null) System.err.println("[carrotmods] move packet not found; high damage is idle");
    }

    static void onAttack(Player attacker, Entity target) {
        if (!Cfg.I.highDamage && !Cfg.I.god()) return;
        Minecraft mc = Minecraft.getInstance();
        if (attacker != mc.player || ctor == null || failed) return;
        if (!(target instanceof LivingEntity) || !(attacker instanceof LocalPlayer p)) return;
        if (!p.onGround() || p.isInWater() || p.onClimbable()) return;

        Object conn = Compat.field(p, "connection");
        try {
            for (double dy : HOP) {
                Object pkt = withCollisionFlag
                    ? ctor.newInstance(p.getX(), p.getY() + dy, p.getZ(), false, false)
                    : ctor.newInstance(p.getX(), p.getY() + dy, p.getZ(), false);
                if (!Compat.call(conn, new String[] {"send"}, pkt)) { failed = true; return; }
            }
        } catch (Throwable t) {
            failed = true;
            System.err.println("[carrotmods] high damage disabled: " + t);
        }
    }
}
