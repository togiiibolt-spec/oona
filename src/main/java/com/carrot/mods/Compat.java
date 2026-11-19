package com.carrot.mods;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * Name-tolerant lookups for the few 26.2 calls whose spelling I could not verify
 * offline. Matching is by shape (param types), so a rename does not break the build.
 */
final class Compat {
    private static boolean warnedClick;

    private Compat() {}

    static boolean call(Object target, String[] names, Object... args) {
        if (target == null) return false;
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getParameterCount() != args.length || !has(names, m.getName())) continue;
                if (!fits(m.getParameterTypes(), args)) continue;
                try {
                    m.setAccessible(true);
                    m.invoke(target, args);
                    return true;
                } catch (Throwable ignored) {
                    // try the next candidate
                }
            }
        }
        return false;
    }

    /** Calls a public/declared no-arg method by name and returns its value (null on any failure). */
    static Object get(Object target, String name) {
        if (target == null) return null;
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getParameterCount() != 0 || !m.getName().equals(name)) continue;
                try {
                    m.setAccessible(true);
                    return m.invoke(target);
                } catch (Throwable ignored) {
                    // try the next candidate
                }
            }
        }
        return null;
    }

    /** First field whose type has this simple name, regardless of what the field is called. */
    static Object fieldOfType(Object target, String simpleTypeName) {
        if (target == null) return null;
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (!f.getType().getSimpleName().equals(simpleTypeName)) continue;
                try {
                    f.setAccessible(true);
                    return f.get(target);
                } catch (Throwable ignored) {
                    // keep looking
                }
            }
        }
        return null;
    }

    static Object field(Object target, String name) {
        if (target == null) return null;
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException ignored) {
                // walk up
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    static boolean setInt(Object target, String name, int value) {
        if (target == null) return false;
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.setInt(target, value);
                return true;
            } catch (NoSuchFieldException ignored) {
                // walk up
            } catch (Throwable t) {
                return false;
            }
        }
        return false;
    }

    static void overlay(Minecraft mc, String text) {
        Component c = Component.literal(text);
        if (call(mc.gui, new String[] {"setOverlayMessage"}, c, Boolean.FALSE)) return;
        call(mc.player, new String[] {"displayClientMessage", "sendOverlayMessage"}, c, Boolean.TRUE);
    }

    static int selected(Inventory inv) {
        try {
            Method m = inv.getClass().getMethod("getSelectedSlot");
            return (Integer) m.invoke(inv);
        } catch (Throwable ignored) {
            // fall back to the old field
        }
        try {
            Field f = Inventory.class.getDeclaredField("selected");
            f.setAccessible(true);
            return f.getInt(inv);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    static void selectHotbar(Inventory inv, int slot) {
        if (call(inv, new String[] {"setSelectedSlot"}, slot)) return;
        try {
            Field f = Inventory.class.getDeclaredField("selected");
            f.setAccessible(true);
            f.setInt(inv, slot);
        } catch (Throwable ignored) {
            // nothing else to try
        }
    }

    /**
     * ClickType.SWAP: swaps the clicked menu slot with a hotbar slot (button 0-8)
     * or the offhand (button 40). Menu ids: 9-35 main, 36-44 hotbar.
     */
    static boolean clickSwap(Minecraft mc, Player p, int menuSlot, int button) {
        Object gm = mc.gameMode;
        if (gm == null) return false;
        for (Class<?> c = gm.getClass(); c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                Class<?>[] t = m.getParameterTypes();
                if (t.length != 5 || t[0] != int.class || t[1] != int.class || t[2] != int.class
                    || !t[3].isEnum() || !t[4].isAssignableFrom(p.getClass())) continue;
                Object swap = null;
                for (Object k : t[3].getEnumConstants()) {
                    if (((Enum<?>) k).name().equals("SWAP")) swap = k;
                }
                if (swap == null) continue;
                try {
                    m.setAccessible(true);
                    m.invoke(gm, p.inventoryMenu.containerId, menuSlot, button, swap, p);
                    return true;
                } catch (Throwable ignored) {
                    // fall through to the warning
                }
            }
        }
        if (!warnedClick) {
            warnedClick = true;
            System.err.println("[carrotmods] no inventory-click method matched; slot swaps are idle");
        }
        return false;
    }

    private static boolean has(String[] names, String n) {
        for (String s : names) if (s.equals(n)) return true;
        return false;
    }

    private static boolean fits(Class<?>[] types, Object[] args) {
        for (int i = 0; i < types.length; i++) {
            if (args[i] == null) {
                if (types[i].isPrimitive()) return false;
                continue;
            }
            if (!box(types[i]).isInstance(args[i])) return false;
        }
        return true;
    }

    private static Class<?> box(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == int.class) return Integer.class;
        if (c == boolean.class) return Boolean.class;
        if (c == float.class) return Float.class;
        if (c == double.class) return Double.class;
        if (c == long.class) return Long.class;
        return c;
    }
}
