package com.carrot.mods;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class Xray {
    /** read from chunk-meshing worker threads */
    public static volatile boolean enabled;

    private static final Map<Block, Boolean> CACHE = new ConcurrentHashMap<>();

    private Xray() {}

    public static boolean isTarget(BlockState state) {
        return CACHE.computeIfAbsent(state.getBlock(), Xray::compute);
    }

    private static boolean compute(Block b) {
        String path = BuiltInRegistries.BLOCK.getKey(b).getPath();
        for (String k : Cfg.I.xrayBlocks) {
            if (path.contains(k)) return true;
        }
        return false;
    }

    static void toggle(Minecraft mc) {
        enabled = !enabled;
        Cfg.I.xray = enabled;
        CACHE.clear();
        reloadChunks(mc);
    }

    /** Rebuild every chunk mesh so the face filter applies; found by type, not by field name. */
    static void reloadChunks(Minecraft mc) {
        Object renderer = Compat.fieldOfType(mc, "LevelRenderer");
        if (!Compat.call(renderer, new String[] {"allChanged"})) {
            Compat.call(renderer, new String[] {"needsUpdate"});
        }
    }
}
