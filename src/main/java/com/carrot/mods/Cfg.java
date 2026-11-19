package com.carrot.mods;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/** Settings live in config/carrotmods.json. Edit the file, restart or re-toggle. */
final class Cfg {
    static Cfg I = new Cfg();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // module switches
    boolean autoTotem, aimbot, xray, autoBridge;
    boolean aimControl, highDamage, reach, autoClutch, speedMine;
    /** master switch: turns on every PvP ability below at once */
    boolean pvpGod;
    boolean autoPots, bedWars;
    /** ultra master: PvP God + Auto Totem + Auto Pots + BedWars Assist */
    boolean variety;
    /** bundled packs copied into shaderpacks/ and resourcepacks/ once */
    boolean seeded;
    boolean newTitle = true;   // Feather-style title screen instead of Carrot's
    boolean pageTheme = true;  // dark theme over vanilla pages

    // auto totem
    float totemHealth = 20f;   // only swap when health <= this (20 = always keep one in offhand)
    int totemDelay = 1;        // ticks between swaps

    // aimbot / aim control
    double aimRange = 4.8;
    float aimSpeed = 20f;      // max degrees per tick
    float aimFov = 140f;       // total cone, degrees
    boolean aimPlayers = true;
    boolean aimMobs = true;
    boolean aimHead = false;
    boolean aimLineOfSight = true;
    boolean aimOnlyWhileAttack = true;
    float aimSmooth = 0.35f;   // aim control: fraction of the remaining angle closed per tick
    float aimJitter = 0.6f;    // aim control: random wobble in degrees
    boolean aimLockTarget = true;
    String aimPriority = "crosshair";   // crosshair | distance | health

    // auto bridge
    boolean bridgeAutoSelect = true;
    int bridgeDelay = 1;
    double bridgeLead = 2.0;   // look-ahead in ticks of current velocity

    // reach (server still enforces its own limit plus a small buffer)
    double reachEntity = 4.0;  // vanilla 3.0
    double reachBlock = 5.5;   // vanilla 4.5

    // auto clutch
    float clutchMinFall = 4f;  // blocks fallen before a clutch is attempted
    double clutchReach = 4.4;  // eye distance to the landing block when we act
    boolean clutchPickup = true;
    List<String> clutchItems = new ArrayList<>(List.of(
        "water_bucket", "cobweb", "slime_block", "honey_block", "hay_block"));

    // speed mine
    double mineSpeed = 1.5;    // client break-speed multiplier
    boolean mineNoDelay = true;

    // auto pots
    float potHealth = 10f;     // throw a healing splash at or below this (20 = full bar)
    int potDelay = 12;         // ticks between throws
    List<String> potMatch = new ArrayList<>(List.of("Healing"));   // matched against the item name

    // xray extras
    boolean xrayGlow = true;       // sparkle particles on every ore in range
    boolean xrayFullbright = true; // client night vision while xray is on
    boolean xrayRadar = true;      // action-bar line pointing at the nearest ore

    // xray: any block whose id path contains one of these is drawn, everything else is hidden
    List<String> xrayBlocks = new ArrayList<>(List.of(
        "_ore", "ancient_debris", "spawner", "chest", "lava", "diamond_block",
        "emerald_block", "gold_block", "netherite_block", "beacon", "end_portal"));

    boolean god() { return pvpGod || variety; }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("carrotmods.json");
    }

    static void load() {
        try {
            Path f = file();
            if (Files.exists(f)) {
                Cfg c = GSON.fromJson(Files.readString(f), Cfg.class);
                if (c != null) I = c;
            }
        } catch (Exception e) {
            System.err.println("[carrotmods] config unreadable, using defaults: " + e);
        }
        save();
    }

    static void save() {
        try {
            Files.writeString(file(), GSON.toJson(I));
        } catch (Exception e) {
            System.err.println("[carrotmods] config not saved: " + e);
        }
    }
}
