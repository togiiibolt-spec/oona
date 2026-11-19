package com.carrot.mods.mixin;

import com.carrot.mods.PageFx;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hooks the end of every screen's background so PageFx can theme it. */
@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "extractBackground", at = @At("TAIL"), require = 0)
    private void carrotmods$theme(GuiGraphicsExtractor g, int mx, int my, float dt, CallbackInfo ci) {
        PageFx.apply((Screen) (Object) this, g);
    }
}
