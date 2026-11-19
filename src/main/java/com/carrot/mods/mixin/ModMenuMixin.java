package com.carrot.mods.mixin;

import com.carrot.mods.MenuHook;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Adds our toggles to Carrot Client's mod menu by widening the List.of(...) in build(). */
@Mixin(targets = "com.carrot.ModMenuScreen")
public abstract class ModMenuMixin {
    @WrapOperation(
        method = "build",
        at = @At(value = "INVOKE", target = "Ljava/util/List;of([Ljava/lang/Object;)Ljava/util/List;"),
        require = 0)
    private List<?> carrotmods$addEntries(Object[] mods, Operation<List<?>> original) {
        return original.call((Object) MenuHook.extend(mods));
    }
}
