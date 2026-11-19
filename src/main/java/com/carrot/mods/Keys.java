package com.carrot.mods;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Raw key polling for combos the keybind system can't express (Ctrl+S). */
final class Keys {
    private Keys() {}

    static boolean down(Minecraft mc, int glfwKey) {
        long handle = windowHandle(mc);
        return handle != 0L && GLFW.glfwGetKey(handle, glfwKey) == GLFW.GLFW_PRESS;
    }

    /** Window.handle() on newer builds, the raw long on older ones. */
    private static long windowHandle(Minecraft mc) {
        Object w = Compat.get(mc, "getWindow");
        if (w instanceof Long l) return l;
        for (String n : new String[] {"handle", "getWindow"}) {
            Object h = Compat.get(w, n);
            if (h instanceof Long l) return l;
        }
        return 0L;
    }
}
