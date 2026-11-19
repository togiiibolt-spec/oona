package com.carrot.mods.mixin;

import com.carrot.mods.Xray;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Sodium replaces vanilla chunk meshing, so vanilla's Block.shouldRenderFace never runs once
 * Sodium is loaded. This is the same xray filter on Sodium's own face test.
 */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext")
public abstract class SodiumXrayMixin {
    @Shadow protected BlockState state;

    @ModifyReturnValue(method = "shouldDrawSide", at = @At("RETURN"), require = 0)
    private boolean carrotmods$draw(boolean original, Direction side) {
        return Xray.enabled ? Xray.isTarget(this.state) : original;
    }

    @ModifyReturnValue(method = "isFaceCulled", at = @At("RETURN"), require = 0)
    private boolean carrotmods$culled(boolean original, Direction side) {
        return Xray.enabled ? !Xray.isTarget(this.state) : original;
    }
}
