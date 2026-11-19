package com.carrot.mods;

import net.minecraft.client.Minecraft;

/**
 * The only place that builds screens depending on Carrot's own classes. They live here, not in
 * CarrotMods, because the JVM checks a class's whole body when it is linked: if CarrotMods
 * mentioned LibraryScreen directly, the standalone jar (no Carrot classes) would fail to load
 * at all. Here, the same failure happens on first use, inside the callers' try/catch.
 */
final class Screens {
    private Screens() {}

    static void openLibrary(Minecraft mc) {
        mc.gui.setScreen(new LibraryScreen(mc.gui.screen()));
    }

    static void swapTitle(Minecraft mc) {
        mc.gui.setScreen(new FeatherTitle());
    }
}
