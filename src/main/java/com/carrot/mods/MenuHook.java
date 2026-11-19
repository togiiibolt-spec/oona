package com.carrot.mods;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Called from ModMenuMixin while Carrot's mod menu builds its card list. The menu's Mod
 * record (name, glyph, category, getter, setter) is built by reflection, so no compile
 * dependency on the original jar is needed.
 */
public final class MenuHook {
    private MenuHook() {}

    public static Object[] extend(Object[] original) {
        if (original == null || original.length == 0) return original;
        try {
            Constructor<?> ctor = original.getClass().getComponentType().getDeclaredConstructors()[0];
            ctor.setAccessible(true);

            List<Object> extra = new ArrayList<>();
            for (Modules.Entry e : Modules.ALL) {
                BooleanSupplier get = e.get();
                Consumer<Boolean> set = v -> {
                    e.set().accept(v);
                    Cfg.save();
                };
                extra.add(ctor.newInstance(e.name(), e.glyph(), e.cat(), get, set));
            }

            Object[] out = Arrays.copyOf(original, original.length + extra.size());
            for (int i = 0; i < extra.size(); i++) out[original.length + i] = extra.get(i);
            return out;
        } catch (Throwable t) {
            System.err.println("[carrotmods] mod menu entries not added: " + t);
            return original;
        }
    }
}
