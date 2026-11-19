package com.carrot.mods;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/** Shader and resource pack library: bundled packs, import, and switching. */
final class Library {
    private static final String[] BUNDLED_SHADERS = {
        "ComplementaryReimagined_r5_9_3.zip", "Solas_Shader_V3_7b.zip"};
    private static final String[] BUNDLED_PACKS = {
        "CarrotCombatPack.zip", "enchant-icons-26_2-v1_3.zip", "_1_6__Enhanced_Boss_Bars.zip"};

    private Library() {}

    static Path shaders() { return FabricLoader.getInstance().getGameDir().resolve("shaderpacks"); }
    static Path packs() { return FabricLoader.getInstance().getGameDir().resolve("resourcepacks"); }
    private static Path irisFile() { return FabricLoader.getInstance().getConfigDir().resolve("iris.properties"); }

    /** First launch only: copies the packs shipped inside the jar next to the game's own pack folders. */
    static void seed() {
        if (Cfg.I.seeded) return;
        copyBundled(BUNDLED_SHADERS, shaders());
        copyBundled(BUNDLED_PACKS, packs());
        Cfg.I.seeded = true;
        Cfg.save();
    }

    private static void copyBundled(String[] names, Path dir) {
        for (String n : names) {
            try (InputStream in = Library.class.getResourceAsStream("/carrotmods/library/" + n)) {
                if (in == null) continue;
                Files.createDirectories(dir);
                Path target = dir.resolve(n);
                if (!Files.exists(target)) Files.copy(in, target);
            } catch (IOException e) {
                System.err.println("[carrotmods] could not copy " + n + ": " + e);
            }
        }
    }

    static List<String> list(Path dir) {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip") || Files.isDirectory(p))
                .map(p -> p.getFileName().toString())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    // ---- shaders (through Iris) ----

    private static final String NO_IRIS =
        "Iris is not loaded. Use the Oona jar (it carries Iris and Sodium).";

    private static Object irisConfig() throws ReflectiveOperationException {
        return Class.forName("net.irisshaders.iris.Iris").getMethod("getIrisConfig").invoke(null);
    }

    static String currentShader() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) return "";
        try {
            Object cfg = irisConfig();
            boolean on = (Boolean) cfg.getClass().getMethod("areShadersEnabled").invoke(cfg);
            if (!on) return "";
            Object name = cfg.getClass().getMethod("getShaderPackName").invoke(cfg);
            return name instanceof java.util.Optional<?> o && o.isPresent() ? String.valueOf(o.get()) : "";
        } catch (Throwable t) {
            return "";
        }
    }

    /** Sets Iris' live config, saves it, then asks Iris to reload; a file edit alone is never re-read. */
    static String applyShader(String name, boolean on) {
        if (!FabricLoader.getInstance().isModLoaded("iris")) return NO_IRIS;
        try {
            Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
            Object cfg = iris.getMethod("getIrisConfig").invoke(null);
            Class<?> cc = cfg.getClass();
            if (on) cc.getMethod("setShaderPackName", String.class).invoke(cfg, name);
            cc.getMethod("setShadersEnabled", boolean.class).invoke(cfg, on);
            cc.getMethod("save").invoke(cfg);
            iris.getMethod("reload").invoke(null);
            return on ? "Shader on: " + name : "Shaders off";
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable c = e.getCause() == null ? e : e.getCause();
            return "Iris could not load it: " + c.getClass().getSimpleName() + " " + c.getMessage();
        } catch (Throwable t) {
            return "Iris call failed: " + t.getClass().getSimpleName();
        }
    }

    // ---- resource packs ----

    static Set<String> activePackFiles(Minecraft mc) {
        Set<String> out = new LinkedHashSet<>();
        Object ids = Compat.get(Compat.get(mc, "getResourcePackRepository"), "getSelectedIds");
        if (ids instanceof Collection<?> c) {
            for (Object o : c) {
                String id = String.valueOf(o);
                out.add(id.startsWith("file/") ? id.substring(5) : id);
            }
        }
        return out;
    }

    static String togglePack(Minecraft mc, String file) {
        Object repo = Compat.get(mc, "getResourcePackRepository");
        if (repo == null) return "Resource pack list unavailable";
        Compat.call(repo, new String[] {"reload"});

        List<String> selected = new ArrayList<>();
        Object ids = Compat.get(repo, "getSelectedIds");
        if (ids instanceof Collection<?> c) {
            for (Object o : c) selected.add(String.valueOf(o));
        }
        String id = "file/" + file;
        boolean wasOn = selected.remove(id);
        if (!wasOn) selected.add(id);

        if (!Compat.call(repo, new String[] {"setSelected"}, selected)) return "Could not change resource packs";
        if (!Compat.call(mc.options, new String[] {"updateResourcePacks"}, repo)) {
            Compat.call(mc, new String[] {"reloadResourcePacks"});
        }
        return (wasOn ? "Disabled " : "Enabled ") + file;
    }

    // ---- adding your own ----

    /** Drag-and-drop target: shader zips go to shaderpacks, resource pack zips to resourcepacks. */
    static String importFile(Path src) {
        String name = src.getFileName().toString();
        if (!name.toLowerCase(Locale.ROOT).endsWith(".zip")) return "Only .zip files can be added (" + name + ")";
        boolean shader = false;
        boolean pack = false;
        try (ZipFile z = new ZipFile(src.toFile())) {
            var entries = z.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String n = e.getName();
                if (n.startsWith("shaders/")) shader = true;
                if (n.equals("pack.mcmeta")) pack = true;
            }
        } catch (IOException e) {
            return "Could not read " + name;
        }
        Path dir = shader ? shaders() : pack ? packs() : null;
        if (dir == null) return "Not a shader or resource pack: " + name;
        try {
            Files.createDirectories(dir);
            Files.copy(src, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            return "Could not copy " + name;
        }
        return "Added " + name + (shader ? " to shaders" : " to resource packs");
    }

    static void openFolder(boolean shaderTab) {
        Path dir = shaderTab ? shaders() : packs();
        try {
            Files.createDirectories(dir);
            Object platform = Class.forName("net.minecraft.Util").getMethod("getPlatform").invoke(null);
            if (!Compat.call(platform, new String[] {"openPath"}, dir)) {
                Compat.call(platform, new String[] {"openFile"}, dir.toFile());
            }
        } catch (Throwable t) {
            System.err.println("[carrotmods] could not open " + dir + ": " + t);
        }
    }
}
