package me.guivnf.mods.hats.common.hat.placement;

import com.google.gson.*;
import me.guivnf.mods.hats.HatsMod;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.nio.file.*;
import java.util.Map;

public class HatPlacementJsonLoader
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Path getPlacementDir()
    {
        return HatsMod.configDirectory.resolve("entity_placement");
    }

    private static Path fileFor(ResourceLocation id)
    {
        return getPlacementDir().resolve(id.getNamespace()).resolve(id.getPath() + ".json");
    }


    public static void load()
    {
        loadBuiltIn();
        loadConfigDir();
        migrateLegacy();
    }

    public static void saveOne(ResourceLocation id, HatPlacementInfo info)
    {
        Path file = fileFor(id);
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(toJson(info), w);
            }
        } catch (IOException e) {
            HatsMod.LOGGER.error("[Hats] Failed to save placement for {}: {}", id, e.getMessage());
        }
    }

    public static void exportAll()
    {
        for (Map.Entry<ResourceLocation, HatPlacementInfo> e : HatPlacementRegistry.getOverrideEntries()) {
            saveOne(e.getKey(), e.getValue());
        }
        HatsMod.LOGGER.info("[Hats] Exported placement overrides to {}", getPlacementDir());
    }


    private static void loadBuiltIn()
    {
        try {
            var url = HatPlacementJsonLoader.class.getClassLoader()
                .getResource("data/hats/hat_placement");
            if (url == null) return;

            Path root;
            if (url.getProtocol().equals("jar")) {
                var jarUri = new java.net.URI(url.toString().split("!")[0].replace("jar:", ""));
                var fs = FileSystems.newFileSystem(jarUri, Map.of());
                root = fs.getPath("data/hats/hat_placement");
            } else {
                root = Path.of(url.toURI());
            }

            loadFromTree(root, "built-in");
        } catch (Exception e) {
            HatsMod.LOGGER.debug("[Hats] No built-in placement data ({})", e.getMessage());
        }
    }

    private static void loadConfigDir()
    {
        Path dir = getPlacementDir();
        if (!Files.isDirectory(dir)) return;
        loadFromTree(dir, "config");
    }

    private static void loadFromTree(Path root, String source)
    {
        try (var stream = Files.walk(root)) {
            stream.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                Path rel = root.relativize(p);
                if (rel.getNameCount() != 2) return;
                String ns   = rel.getName(0).toString();
                String path = rel.getName(1).toString().replace(".json", "");
                ResourceLocation id = ResourceLocation.tryParse(ns + ":" + path);
                if (id == null) return;
                loadFile(p, id, source);
            });
        } catch (IOException e) {
            HatsMod.LOGGER.warn("[Hats] Failed walking placement tree {}: {}", root, e.getMessage());
        }
    }

    private static void loadFile(Path file, ResourceLocation id, String source)
    {
        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject obj = GSON.fromJson(r, JsonObject.class);
            if (obj == null) return;
            HatPlacementRegistry.registerOverride(id, fromJson(obj));
        } catch (Exception e) {
            HatsMod.LOGGER.error("[Hats] Failed to load placement file {}: {}", file, e.getMessage());
        }
    }

    private static void migrateLegacy()
    {
        Path legacy = HatsMod.configDirectory.resolve("entity_placement.json");
        Path legacyMods = HatsMod.hatsDirectory.resolve("entity_placement.json");

        for (Path candidate : new Path[]{ legacy, legacyMods }) {
            if (!Files.exists(candidate)) continue;
            HatsMod.LOGGER.info("[Hats] Migrating legacy placement file: {}", candidate);
            try (Reader r = Files.newBufferedReader(candidate)) {
                JsonObject root = GSON.fromJson(r, JsonObject.class);
                if (root != null) {
                    for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                        if (!e.getValue().isJsonObject()) continue;
                        ResourceLocation id = ResourceLocation.tryParse(e.getKey());
                        if (id == null) continue;
                        HatPlacementInfo info = fromJson(e.getValue().getAsJsonObject());
                        HatPlacementRegistry.registerOverride(id, info);
                        saveOne(id, info);
                    }
                }
                Files.move(candidate, candidate.resolveSibling("entity_placement.json.migrated"),
                    StandardCopyOption.REPLACE_EXISTING);
                HatsMod.LOGGER.info("[Hats] Migration complete. Old file renamed to *.migrated");
            } catch (Exception e) {
                HatsMod.LOGGER.error("[Hats] Legacy migration failed: {}", e.getMessage());
            }
        }
    }


    private static HatPlacementInfo fromJson(JsonObject obj)
    {
        float offsetX = getFloat(obj, "offsetX",     getFloat(obj, "headOffsetX", 0f));
        float offsetY = getFloat(obj, "offsetY",     getFloat(obj, "headOffsetY", 0f));
        float offsetZ = getFloat(obj, "offsetZ",     getFloat(obj, "headOffsetZ", 0f));
        float rotX    = getFloat(obj, "rotX",    0f);
        float rotY    = getFloat(obj, "rotY",    0f);
        float rotZ    = getFloat(obj, "rotZ",    0f);
        float scale   = getFloat(obj, "scale",   1f);
        boolean boss  = obj.has("isBoss") && obj.get("isBoss").getAsBoolean();
        return new HatPlacementInfo(offsetX, offsetY, offsetZ, rotX, rotY, rotZ, scale, boss);
    }

    private static JsonObject toJson(HatPlacementInfo info)
    {
        JsonObject obj = new JsonObject();
        obj.addProperty("offsetX", info.offsetX);
        obj.addProperty("offsetY", info.offsetY);
        obj.addProperty("offsetZ", info.offsetZ);
        if (info.rotX != 0f) obj.addProperty("rotX", info.rotX);
        if (info.rotY != 0f) obj.addProperty("rotY", info.rotY);
        if (info.rotZ != 0f) obj.addProperty("rotZ", info.rotZ);
        obj.addProperty("scale",   info.scale);
        if (info.isBoss) obj.addProperty("isBoss", true);
        return obj;
    }

    private static float getFloat(JsonObject obj, String key, float def)
    {
        return obj.has(key) ? obj.get(key).getAsFloat() : def;
    }
}
