package com.carrot.mods.mixin;

import com.carrot.mods.Xray;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Block.class)
public abstract class BlockMixin {
    /**
     * Face culling hook. Matches every shouldRenderFace overload by name; each
     * one starts with the BlockState being drawn. Xray on: targets draw all
     * faces, everything else draws none.
     */
    @ModifyReturnValue(method = "shouldRenderFace", at = @At("RETURN"), require = 0)
    private static boolean carrotmods$xray(boolean original, BlockState state) {
        return Xray.enabled ? Xray.isTarget(state) : original;
    }
}
