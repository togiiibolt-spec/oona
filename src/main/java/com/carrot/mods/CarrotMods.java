package com.carrot.mods;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFW;

public final class CarrotMods implements ClientModInitializer {
    private static final KeyMapping[] KEYS = new KeyMapping[Modules.ALL.size()];
    private static KeyMapping libraryKey;
    private static boolean comboHeld;
    private static int titleTicks;

    @Override
    public void onInitializeClient() {
        Cfg.load();
        Xray.enabled = Cfg.I.xray;
        HighDamage.init();
        try {
            Library.seed();
        } catch (Throwable t) {
            System.err.println("[carrotmods] library seed failed: " + t);
        }

        KeyMapping.Category cat = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("carrotmods", "main"));
        for (int i = 0; i < KEYS.length; i++) {
            Modules.Entry e = Modules.ALL.get(i);
            KEYS[i] = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.carrotmods." + e.id(), InputConstants.Type.KEYSYM, e.key(), cat));
        }
        libraryKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.carrotmods.library", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, cat));

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            HighDamage.onAttack(player, entity);
            return InteractionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(CarrotMods::tick);
    }

    private static void tick(Minecraft mc) {
        // the game rewrites its own window title when the world or screen changes; put ours back
        if (titleTicks++ % 100 == 0) {
            Compat.call(Compat.get(mc, "getWindow"), new String[] {"setTitle"}, "Oona 26.2");
        }

        for (int i = 0; i < KEYS.length; i++) {
            Modules.Entry e = Modules.ALL.get(i);
            while (KEYS[i].consumeClick()) {
                e.set().accept(!e.get().getAsBoolean());
                Cfg.save();
                if (mc.player != null) {
                    Compat.overlay(mc, e.name() + ": " + (e.get().getAsBoolean() ? "ON" : "OFF"));
                }
            }
        }
        while (libraryKey.consumeClick()) openLibrary(mc);

        // Carrot swaps the vanilla title for its own; swap that one for the new title
        net.minecraft.client.gui.screens.Screen current = mc.gui.screen();
        if (Cfg.I.newTitle && current != null && current.getClass().getName().equals("com.carrot.CarrotTitleScreen")) {
            try {
                mc.gui.setScreen(new FeatherTitle());
            } catch (Throwable t) {
                Cfg.I.newTitle = false;   // fall back to Carrot's title instead of retrying every tick
                System.err.println("[carrotmods] new title failed, using Carrot's: " + t);
            }
        }

        // Ctrl+S in a world opens the library (edge triggered)
        boolean combo = mc.gui.screen() == null
            && (Keys.down(mc, GLFW.GLFW_KEY_LEFT_CONTROL) || Keys.down(mc, GLFW.GLFW_KEY_RIGHT_CONTROL))
            && Keys.down(mc, GLFW.GLFW_KEY_S);
        if (combo && !comboHeld) openLibrary(mc);
        comboHeld = combo;

        if (mc.player == null || mc.level == null) {
            Reach.tick(mc, false);
            SpeedMine.tick(mc, false);
            AutoClutch.reset();
            XrayFx.tick(mc);
            return;
        }

        Cfg c = Cfg.I;
        boolean god = c.god();
        boolean variety = c.variety;
        Reach.tick(mc, god || c.reach);
        SpeedMine.tick(mc, god || c.speedMine);
        if (god || c.autoClutch) AutoClutch.tick(mc); else AutoClutch.reset();
        if (god || c.autoBridge) AutoBridge.tick(mc);
        if (c.autoTotem || variety) AutoTotem.tick(mc);
        if (c.autoPots || variety) AutoPots.tick(mc);
        if (c.bedWars || variety) BedWars.tick(mc);
        if (god || c.aimbot || c.aimControl) Aimbot.tick(mc, god || c.aimControl);
        XrayFx.tick(mc);
    }

    private static void openLibrary(Minecraft mc) {
        try {
            mc.gui.setScreen(new LibraryScreen(mc.gui.screen()));
        } catch (Throwable t) {
            // standalone jar: Carrot's screen base is missing
            if (mc.player != null) Compat.overlay(mc, "Library needs the Oona jar");
        }
    }
}
